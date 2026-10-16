package org.zotero.android.attachmentdownloader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.ZoteroNoRedirectApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.Attachment.FileLinkType
import org.zotero.android.database.objects.Attachment.FileLocation
import org.zotero.android.database.objects.Attachment.Kind
import org.zotero.android.database.requests.MarkFileAsDownloadedDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.Unzipper
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.WebDavSessionStorage
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton as it's used by Controller
@Singleton
class AttachmentDownloader @Inject constructor(
    private val zoteroApi: ZoteroApi,
    private val zoteroNoRedirectApi: ZoteroNoRedirectApi,
    private val fileStorage: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    dispatcher: CoroutineDispatcher,
    private val unzipper: Unzipper,
    private val webDavController: WebDavController,
    private val sessionStorage: WebDavSessionStorage,
) {
    sealed class Error : Exception() {
        object incompatibleAttachment : Error()
        object zipDidntContainRequestedFile : Error()
        object cantUnzipSnapshot : Error()
    }

    data class Download(
        val key: String,
        val libraryId: LibraryIdentifier,
    )

    data class Update(
        val key: String,
        val parentKey: String?,
        val libraryId: LibraryIdentifier,
        val kind: Kind,
    ) {
        sealed class Kind {
            data class progress(val progressInHundreds: Int) : Kind()
            object ready : Kind()
            data class failed(val exception: Throwable) : Kind()
            object cancelled : Kind()
        }


        companion object {
            fun init(
                key: String,
                parentKey: String?,
                libraryId: LibraryIdentifier,
                kind: Kind
            ): Update {
                return Update(
                    key = key,
                    parentKey = parentKey,
                    libraryId = libraryId,
                    kind = kind,
                )

            }

            fun init(download: Download, parentKey: String?, kind: Kind): Update {
                return Update(
                    key = download.key,
                    parentKey = parentKey,
                    libraryId = download.libraryId,
                    kind = kind,
                )
            }
        }
    }

    // Progress of downloads of more than one attachment, updated as each attachment finishes
    sealed interface BatchState {
        // Attachments to download are being determined
        object Preparing : BatchState
        data class Downloading(val downloaded: Int, val total: Int) : BatchState
    }

    private var userId: Long = 0L
    private var coroutineScope = CoroutineScope(dispatcher)
    // Modified both when downloads are enqueued and when they finish on download threads
    private var operations = ConcurrentHashMap<Download, AttachmentDownloadOperation>()
    private var errors = ConcurrentHashMap<Download, Throwable>()
    private val preparingBatchCount = AtomicInteger(0)
    private val batchStateFlow = MutableStateFlow<BatchState?>(null)
    val batchState: StateFlow<BatchState?> = batchStateFlow.asStateFlow()
    private var batchProgress: AttachmentBatchProgress = AttachmentBatchProgress()
    private var totalBatchCount: Int = 0

    val batchData: Triple<Int?, Int, Int>
        get() {
            val progress = this.batchProgress.currentProgress
            val totalBatchCount = this.totalBatchCount
            val remainingBatchCount = this.operations.size
            return Triple(progress, remainingBatchCount, totalBatchCount)
        }

    fun init(userId: Long) {
        this.userId = userId
        operations.clear()
        errors.clear()
        batchProgress = AttachmentBatchProgress()
    }

    fun batchDownload(attachments: List<Pair<Attachment, String?>>) {
        val operations = mutableListOf<Triple<Download, String?, AttachmentDownloadOperation>>()
        for (attach in attachments) {
            val attachment = attach.first
            val parentKey = attach.second

            when (attachment.type) {
                is Kind.url -> {
                    //no-op
                }

                is Kind.file -> {
                    val filename = attachment.type.filename
                    val contentType = attachment.type.contentType
                    val location = attachment.type.location
                    val linkType = attachment.type.linkType
                    when (linkType) {
                        FileLinkType.linkedFile, FileLinkType.embeddedImage -> {
                            //no-op
                        }

                        FileLinkType.importedFile, FileLinkType.importedUrl -> {
                            when (location) {
                                FileLocation.local -> {
                                    //no-op
                                }

                                FileLocation.remote, FileLocation.remoteMissing, FileLocation.localAndChangedRemotely -> {
                                    Timber.i("AttachmentDownloader: batch download remote${if (location == FileLocation.remoteMissing) "ly missing" else ""} file ${attachment.key}")
                                    val file = fileStorage.attachmentFile(
                                        attachment.libraryId,
                                        key = attachment.key,
                                        filename = filename
                                    )
                                    val createDownloadResult = createDownload(
                                        file = file,
                                        key = attachment.key,
                                        parentKey = parentKey,
                                        libraryId = attachment.libraryId,
                                        hasLocalCopy = false
                                    )
                                    if (createDownloadResult == null) {
                                        continue
                                    }
                                    operations.add(
                                        Triple(
                                            createDownloadResult.first,
                                            parentKey,
                                            createDownloadResult.second
                                        )
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }

        for (data in operations) {
            Timber.i("AttachmentDownloader: enqueue ${data.first.key}")
            attachmentDownloaderEventStream.emitAsync(
                Update.init(
                    download = data.first,
                    parentKey = data.second,
                    kind = Update.Kind.progress(0)
                )
            )
        }

        val downloadOperations = operations.map { it.third }

        coroutineScope.async {
            downloadOperations.forEach {
                it.start(this)
            }
        }
    }

    fun downloadIfNeeded(attachment: Attachment, parentKey: String?) {
        val attachmentType = attachment.type
        when (attachmentType) {
            is Kind.url -> {
                Timber.i("AttachmentDownloader: open url ${attachment.key}")
                attachmentDownloaderEventStream.emitAsync(
                    Update.init(
                        key = attachment.key,
                        parentKey = parentKey,
                        libraryId = attachment.libraryId,
                        kind = Update.Kind.ready
                    )
                )
            }

            is Kind.file -> {
                val filename = attachmentType.filename
                val location = attachmentType.location
                val linkType = attachmentType.linkType
                when (linkType) {
                    FileLinkType.linkedFile, FileLinkType.embeddedImage -> {
                        Timber.i("AttachmentDownloader: tried opening linkedFile or embeddedImage ${attachment.key}")

                        attachmentDownloaderEventStream.emitAsync(
                            Update.init(
                                key = attachment.key,
                                parentKey = parentKey,
                                libraryId = attachment.libraryId,
                                kind = Update.Kind.failed(
                                    Error.incompatibleAttachment
                                )
                            )
                        )
                    }

                    FileLinkType.importedFile, FileLinkType.importedUrl -> {
                        when (location) {
                            FileLocation.local -> {
                                Timber.i("AttachmentDownloader: open local file ${attachment.key}")

                                attachmentDownloaderEventStream.emitAsync(
                                    Update.init(
                                        key = attachment.key,
                                        parentKey = parentKey,
                                        libraryId = attachment.libraryId,
                                        kind = Update.Kind.ready
                                    )
                                )
                            }

                            FileLocation.remote, FileLocation.remoteMissing -> {
                                Timber.i("AttachmentDownloader: download remote${if (location == FileLocation.remoteMissing) "ly missing" else ""} file ${attachment.key}")

                                val file = fileStorage.attachmentFile(
                                    libraryId = attachment.libraryId,
                                    key = attachment.key,
                                    filename = filename,
                                )
                                download(
                                    file = file,
                                    key = attachment.key,
                                    parentKey = parentKey,
                                    libraryId = attachment.libraryId,
                                    hasLocalCopy = false
                                )
                            }

                            FileLocation.localAndChangedRemotely -> {
                                val file = fileStorage.attachmentFile(
                                    libraryId = attachment.libraryId,
                                    key = attachment.key,
                                    filename = filename,
                                )

                                var hasLocalCopy = true

                                if (file.extension == "pdf" && file.exists() && !fileStorage.isPdf(
                                        file = file
                                    )
                                ) {
                                    file.deleteRecursively()
                                    hasLocalCopy = false
                                }

                                if (hasLocalCopy) {
                                    Timber.i("AttachmentDownloader: download local file with remote change ${attachment.key}")
                                } else {
                                    Timber.i("AttachmentDownloader: download remote file ${attachment.key}. Fixed local PDF.")
                                }

                                download(
                                    file = file,
                                    key = attachment.key,
                                    parentKey = parentKey,
                                    libraryId = attachment.libraryId,
                                    hasLocalCopy = hasLocalCopy
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun cancel(key: String, libraryId: LibraryIdentifier) {
        _cancel(key = key, libraryId = libraryId)
    }

    private fun _cancel(key: String, libraryId: LibraryIdentifier) {
        val download = Download(key = key, libraryId = libraryId)
        val operation = this.operations[download]
        if (operation == null) {
            return
        }
        this.operations.remove(download)
        this.batchProgress.finishDownload(key)
        resetBatchDataIfNeeded()

        Timber.i("AttachmentDownloader: cancelled ${download.key}")
        operation.cancel()
    }

    private fun download(
        file: File,
        key: String,
        parentKey: String?,
        libraryId: LibraryIdentifier,
        hasLocalCopy: Boolean
    ) {
        val pairResult = createDownload(
            file = file,
            key = key,
            parentKey = parentKey,
            libraryId = libraryId,
            hasLocalCopy = hasLocalCopy
        )
        if (pairResult == null) {
            return
        }
        val (download, operation) = pairResult
        enqueue(operation = operation, download = download, parentKey = parentKey)
    }

    private fun enqueue(
        operation: AttachmentDownloadOperation,
        download: Download,
        parentKey: String?
    ) {
        Timber.i("AttachmentDownloader: enqueue ${download.key}")

        attachmentDownloaderEventStream.emitAsync(
            Update.init(download = download, parentKey = parentKey, kind = Update.Kind.progress(0))
        )
        coroutineScope.async {
            operation.start(this)
        }
    }

    private fun createDownload(
        file: File,
        key: String,
        parentKey: String?,
        libraryId: LibraryIdentifier,
        hasLocalCopy: Boolean
    ): Pair<Download, AttachmentDownloadOperation>? {
        val download = Download(key = key, libraryId = libraryId)

        if (operations[download] != null) {
            return null
        }

        val operation = AttachmentDownloadOperation(
            file = file,
            download = download,
            userId = this.userId,
            zoteroApi = zoteroApi,
            zoteroNoRedirectApi = this.zoteroNoRedirectApi,
            unzipper = this.unzipper,
            webDavController = webDavController,
            sessionStorage = sessionStorage
        )
        operation.onDownloadProgressUpdated = object : OnDownloadProgressUpdated {
            override fun onProgressUpdated(progressInHundreds: Int) {
                batchProgress.updateProgress(key, progressInHundreds)
                attachmentDownloaderEventStream.emitAsync(
                    Update.init(
                        download = download,
                        parentKey = parentKey,
                        kind = Update.Kind.progress(
                            progressInHundreds
                        )
                    )
                )
            }
        }
        operation.finishedDownload = { result ->
            when (result) {
                is CustomResult.GeneralError -> {
                    finish(
                        download = download,
                        parentKey = parentKey,
                        result = result,
                        hasLocalCopy = hasLocalCopy
                    )
                }

                is CustomResult.GeneralSuccess -> {
                    dbWrapperMain.realmDbStorage.perform(
                        request = MarkFileAsDownloadedDbRequest(
                            key = download.key,
                            libraryId = download.libraryId,
                            downloaded = true
                        )
                    )
                    finish(
                        download = download,
                        parentKey = parentKey,
                        result = result,
                        hasLocalCopy = hasLocalCopy
                    )
                }
            }
        }

        this.errors.remove(download)
        this.operations[download] = operation
        this.totalBatchCount += 1
        publishBatchState()

        return download to operation
    }

    private fun finish(
        download: Download,
        parentKey: String?,
        result: CustomResult<Unit>,
        hasLocalCopy: Boolean
    ) {
        _finish(
            download = download,
            parentKey = parentKey,
            result = result,
            hasLocalCopy = hasLocalCopy
        )
    }

    private fun _finish(
        download: Download,
        parentKey: String?,
        result: CustomResult<Unit>,
        hasLocalCopy: Boolean
    ) {
        this.operations.remove(download)
        resetBatchDataIfNeeded()
        publishBatchState()

        when (result) {
            is CustomResult.GeneralError.CodeError -> {
                val isCancelError = result.throwable is AttachmentDownloadOperation.Error.cancelled
                if (isCancelError || hasLocalCopy) {
                    this.errors.remove(download)
                } else {
                    this.errors[download] = result.throwable
                }
                if (isCancelError) {
                    attachmentDownloaderEventStream.emitAsync(
                        Update.init(
                            download = download,
                            parentKey = parentKey,
                            kind = Update.Kind.cancelled
                        )
                    )
                } else if (hasLocalCopy) {
                    attachmentDownloaderEventStream.emitAsync(
                        Update.init(
                            download = download,
                            parentKey = parentKey,
                            kind = Update.Kind.ready
                        )
                    )
                } else {
                    Timber.e(
                        result.throwable,
                        "AttachmentDownloader: failed to download attachment ${download.key}, ${download.libraryId}"
                    )
                    attachmentDownloaderEventStream.emitAsync(
                        Update.init(
                            download = download,
                            parentKey = parentKey,
                            kind = Update.Kind.failed(result.throwable)
                        )
                    )
                }
            }

            is CustomResult.GeneralError.NetworkError -> {
                if (hasLocalCopy) {
                    this.errors.remove(download)
                } else {
                    this.errors[download] = Exception(result.stringResponse)
                }
                if (hasLocalCopy) {
                    attachmentDownloaderEventStream.emitAsync(
                        Update.init(
                            download = download,
                            parentKey = parentKey,
                            kind = Update.Kind.ready
                        )
                    )
                } else {
                    Timber.e(
                        result.stringResponse,
                        "AttachmentDownloader: failed to download attachment ${download.key}, ${download.libraryId}"
                    )
                    attachmentDownloaderEventStream.emitAsync(
                        Update.init(
                            download = download,
                            parentKey = parentKey,
                            kind = Update.Kind.failed(Exception(result.stringResponse))
                        )
                    )
                }
            }

            is CustomResult.GeneralSuccess -> {
                Timber.i("AttachmentDownloader: finished downloading ${download.key}")
                this.errors.remove(download)
                attachmentDownloaderEventStream.emitAsync(
                    Update.init(
                        download = download,
                        parentKey = parentKey,
                        kind = Update.Kind.ready
                    )
                )
            }

            else -> {}
        }
    }

    fun stop() {
        operations.forEach {
            it.value.cancel()
        }
    }

    fun data(key: String, libraryId: LibraryIdentifier): Pair<Int?, Throwable?> {
        val download = Download(key = key, libraryId = libraryId)
        val progress = this.operations[download]?.progressInHundreds
        val error = this.errors[download]
        return progress to error
    }

    // Call before determining the attachments of a batch download, which can take a while,
    // and again once it's done, whether or not batchDownload() was called
    fun setPreparingBatch(isPreparing: Boolean) {
        if (isPreparing) {
            preparingBatchCount.incrementAndGet()
        } else {
            preparingBatchCount.updateAndGet { maxOf(it - 1, 0) }
        }
        publishBatchState()
    }

    private fun publishBatchState() {
        val total = this.totalBatchCount
        val remaining = this.operations.size
        batchStateFlow.value = when {
            remaining > 0 && total > 1 -> BatchState.Downloading(downloaded = total - remaining, total = total)
            preparingBatchCount.get() > 0 -> BatchState.Preparing
            else -> null
        }
    }

    private fun resetBatchDataIfNeeded() {
        if (this.operations.isEmpty()) {
            this.batchProgress = AttachmentBatchProgress()
            this.totalBatchCount = 0
        }
    }

}