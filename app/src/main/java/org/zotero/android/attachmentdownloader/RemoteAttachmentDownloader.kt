package org.zotero.android.attachmentdownloader

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.objects.Attachment
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetMimeTypeUseCase
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
    private val getMimeTypeUseCase: GetMimeTypeUseCase,
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
            getMimeTypeUseCase = getMimeTypeUseCase,
            noAuthenticationApi = this.noAuthenticationApi,
            fileStorage = this.fileStorage
        )

        operation.onDownloadProgressUpdated = object : OnDownloadProgressUpdated {
            override fun onProgressUpdated(progressInHundreds: Int) {
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
        this.operations[download] = operation
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

}