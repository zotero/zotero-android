package org.zotero.android.attachmentdownloader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.objects.Attachment
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteAttachmentDownloader @Inject constructor(
    private val fileStorage: FileStore,
    private val attachmentDownloaderEventStream: RemoteAttachmentDownloaderEventStream,
    private val noAuthenticationApi: NoAuthenticationApi,
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    val dispatcher: CoroutineDispatcher,
) {
    data class Download(
        val key: String,
        val parentKey: String,
        val libraryId: LibraryIdentifier
    )

    data class Update(
        val download: Download,
        val kind: Kind
    ) {
        sealed class Kind {
            data class progress(val progressInHundreds: Int): Kind()
            data class ready(val attachment: Attachment): Kind()
            object failed: Kind()
            object cancelled: Kind()
        }
    }

    private var operations = mutableMapOf<Download, RemoteAttachmentDownloadOperation>()
    private var errors = mutableMapOf<Download, Throwable>()
    private var coroutineScope = CoroutineScope(dispatcher)
    private var batchProgress: AttachmentBatchProgress = AttachmentBatchProgress()
    private var totalBatchCount: Int = 0

    val batchData: Triple<Int?, Int, Int>
        get() {
            val progress = this.batchProgress.currentProgress
            val remainingBatchCount = this.operations.size
            val totalBatchCount = this.totalBatchCount
            return Triple(progress, remainingBatchCount, totalBatchCount)
        }

    fun data(key: String, parentKey: String, libraryId: LibraryIdentifier): Pair<Int?, Throwable?> {
        val download = Download(key = key, parentKey = parentKey, libraryId = libraryId)
        var progress: Int? = null
        val error = this.errors[download]

        val operation = this.operations[download]
        if (operation != null) {
            val _progress = operation.progressInHundreds
            if (_progress != null) {
                progress = _progress
            } else if (!operation.isOperationNotActive()) {
                progress = 0
            }
        }
        return progress to error
    }

    fun download(data: List<Triple<Attachment, String, String>>) {
        Timber.i("RemoteAttachmentDownloader: enqueue ${data.size} attachments")
        val operations = data.mapNotNull {
            createDownload(
                url = it.second,
                attachment = it.first,
                parentKey = it.third
            )
        }
        operations.forEach { operation ->
            coroutineScope.async {
                operation.start(this)
            }
        }
    }


    private fun createDownload(url: String, attachment: Attachment, parentKey: String): RemoteAttachmentDownloadOperation? {
        val download = Download(key = attachment.key, parentKey = parentKey, libraryId = attachment.libraryId)
        if (operations[download] != null) {
            return null
        }
        val file = file(attachment)
        if (file == null) {
            return null
        }
        val operation = RemoteAttachmentDownloadOperation(
            url = url,
            file = file,
            getUriDetailsUseCase = getUriDetailsUseCase,
            noAuthenticationApi = this.noAuthenticationApi,
            fileStorage = this.fileStorage
        )

        operation.onDownloadProgressUpdated = object : OnDownloadProgressUpdated {
            override fun onProgressUpdated(progressInHundreds: Int) {
                batchProgress.updateProgress(attachment.key, progressInHundreds)
                attachmentDownloaderEventStream.emitAsync(
                    Update(
                        download = download, kind = Update.Kind.progress(
                            progressInHundreds
                        )
                    )
                )
            }
        }
        operation.finishedDownload = { result ->
            finish(
                download = download,
                attachment = attachment,
                result = result
            )
        }
        this.errors.remove(download)
        this.operations[download] = operation
        this.totalBatchCount += 1

        return operation
    }

    private fun file(attachment: Attachment):  File? {
        val attachmentType = attachment.type
        when(attachmentType) {
            is Attachment.Kind.file -> {
                val filename = attachmentType.filename
                return fileStorage.attachmentFile(
                    libraryId = attachment.libraryId,
                    key = attachment.key,
                    filename = filename,
                )
            }
            is Attachment.Kind.url -> {
                return null
            }
        }
    }

    private fun finish(
        download: Download,
        attachment: Attachment,
        result: CustomResult<Unit>
    ) {
        this.operations.remove(download)
        resetBatchDataIfNeeded()
        when (result) {
            is CustomResult.GeneralError.CodeError -> {
                Timber.e(
                    result.throwable,
                    "RemoteAttachmentDownloader: failed to download attachment ${download.key}, ${download.libraryId}"
                )
                val isCancelError =
                    result.throwable is RemoteAttachmentDownloadOperation.Error.cancelled
                if (isCancelError) {
                    this.errors.remove(download)
                } else {
                    this.errors[download] = result.throwable
                }

                if (isCancelError) {
                    attachmentDownloaderEventStream.emitAsync(
                        Update(
                            download = download,
                            kind = Update.Kind.cancelled
                        )
                    )
                } else {
                    attachmentDownloaderEventStream.emitAsync(
                        Update(
                            download = download,
                            kind = Update.Kind.failed
                        )
                    )
                }
            }
            is CustomResult.GeneralError.NetworkError -> {
                this.errors[download] = Exception(result.stringResponse)
                Timber.e(
                    result.stringResponse,
                    "RemoteAttachmentDownloader: failed to download attachment ${download.key}, ${download.libraryId}"
                )
                attachmentDownloaderEventStream.emitAsync(
                    Update(
                        download = download,
                        kind = Update.Kind.failed
                    )
                )
            }

            is CustomResult.GeneralSuccess -> {
                Timber.i("RemoteAttachmentDownloader: finished downloading ${download.key}")

                attachmentDownloaderEventStream.emitAsync(
                    Update(download = download, kind = Update.Kind.ready(attachment))
                )
                this.errors.remove(download)
            }
            else -> {}
        }
    }

    private fun resetBatchDataIfNeeded() {
        if (this.operations.isEmpty()) {
            this.batchProgress = AttachmentBatchProgress()
            this.totalBatchCount = 0
        }
    }

    fun stop() {
        operations.forEach {
            it.value.cancel()
        }
    }

}