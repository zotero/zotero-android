package org.zotero.android.sync.syncactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.zotero.android.api.network.CustomResult
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.database.requests.MarkForResyncDbAction
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.syncactions.architecture.SyncAction
import timber.log.Timber

class UploadFixSyncAction(
    val key: String,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    private val coroutineScope: CoroutineScope,
    private val syncSchedulerSemaphore: Semaphore,
) : SyncAction() {

    sealed class Error : Exception() {
        object attachmentMissingRemotely : Error()
        object fileNotDownloaded : Error()
        object itemNotAttachment : Error()
        data class incorrectAttachmentType(val q: Attachment.Kind) : Error()
        data class incorrectLinkType(val q: Attachment.FileLinkType) : Error()
        object expired : Error()
    }

    private var finishDownload: ((customResult: CustomResult<Unit>) -> Unit)? = null
    private fun isOperationNotActive() =
        !this.coroutineScope.isActive

    suspend fun result() {
        Timber.i("UploadFixSyncAction: fix upload for ${this.key}; ${this.libraryId}")
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                syncSchedulerSemaphore.withPermit {
                    if (update.key != this.key || update.libraryId != this.libraryId) {
                        return@onEach
                    }
                    when (update.kind) {
                        is AttachmentDownloader.Update.Kind.failed -> {
                            this.finishDownload?.let { it(CustomResult.GeneralError.CodeError(update.kind.exception)) }
                            this.finishDownload = null
                        }

                        AttachmentDownloader.Update.Kind.ready -> {
                            this.finishDownload?.let { it(CustomResult.GeneralSuccess(Unit)) }
                            this.finishDownload = null
                        }

                        is AttachmentDownloader.Update.Kind.progress, AttachmentDownloader.Update.Kind.cancelled -> {
                            //no-op
                        }
                    }
                }

            }
            .launchIn(coroutineScope)

        val attachmentResult = fetchAndValidateAttachment()
        if (attachmentResult !is CustomResult.GeneralSuccess) {
            val codeError = attachmentResult as CustomResult.GeneralError.CodeError
            throw codeError.throwable
        }
        val attachment = attachmentResult.value!!
        download(attachment = attachment)
        markAsUploaded()
    }

    private fun markAsUploaded(): CustomResult<Unit> {
        if (isOperationNotActive()) {
            return CustomResult.GeneralError.CodeError(Error.expired)
        }
        try {
            val markAsUploaded = MarkAttachmentUploadedDbRequest(
                libraryId = this.libraryId,
                key = this.key,
                version = null
            )
            val markForResync = MarkForResyncDbAction(
                libraryId = this.libraryId,
                keys = listOf(this.key),
                clazz = RItem::class
            )
            dbWrapperMain.realmDbStorage.perform(listOf(markAsUploaded, markForResync))
            return CustomResult.GeneralSuccess(Unit)
        } catch (e: Exception) {
            return CustomResult.GeneralError.CodeError(e)
        }
    }

    private fun download(attachment: Attachment): CustomResult<Unit> {
        if (isOperationNotActive()) {
            return CustomResult.GeneralError.CodeError(Error.expired)
        }

        this.finishDownload = {
            //TODO
        }
        this.attachmentDownloader.downloadIfNeeded(attachment = attachment, parentKey = null)
        return CustomResult.GeneralSuccess(Unit)
    }


    private fun fetchAndValidateAttachment(): CustomResult<Attachment> {
        if (isOperationNotActive()) {
            return CustomResult.GeneralError.CodeError(Error.expired)
        }

        try {
            val item = dbWrapperMain.realmDbStorage.perform(
                ReadItemDbRequest(
                    libraryId = this.libraryId,
                    key = this.key
                )
            )
            if (item.rawType != ItemTypes.attachment) {
                Timber.e("UploadFixSyncAction: item not attachment - ${item.rawType}")
                return CustomResult.GeneralError.CodeError(Error.itemNotAttachment)
            }
            val attachment = AttachmentCreator.attachment(
                item,
                options = AttachmentCreator.Options.light,
                fileStorage = this.fileStore,
                urlDetector = null,
                isForceRemote = false,
                defaults = this.defaults,
            )
            if (attachment == null) {
                Timber.e("UploadFixSyncAction: item not attachment - ${item.rawType}")
                return CustomResult.GeneralError.CodeError(Error.itemNotAttachment)
            }
            when (val type = attachment.type) {
                is Attachment.Kind.url -> {
                    Timber.e("UploadFixSyncAction: incorrect item type - ${attachment.type}")
                    return CustomResult.GeneralError.CodeError(
                        Error.incorrectAttachmentType(
                            attachment.type
                        )
                    )
                }

                is Attachment.Kind.file -> {
                    when (type.linkType) {
                        Attachment.FileLinkType.embeddedImage, Attachment.FileLinkType.linkedFile -> {
                            Timber.e("UploadFixSyncAction: incorrect link type - ${type.linkType}")
                            return CustomResult.GeneralError.CodeError(Error.incorrectLinkType(type.linkType))
                        }

                        Attachment.FileLinkType.importedFile, Attachment.FileLinkType.importedUrl -> {
                            when (type.location) {
                                Attachment.FileLocation.remoteMissing -> {
                                    Timber.e("UploadFixSyncAction: attachment missing remotely")
                                    return CustomResult.GeneralError.CodeError(Error.attachmentMissingRemotely)
                                }

                                Attachment.FileLocation.local, Attachment.FileLocation.localAndChangedRemotely, Attachment.FileLocation.remote -> {
                                    val newAttachment = Attachment(
                                        type = Attachment.Kind.file(
                                            filename = type.filename,
                                            contentType = type.contentType,
                                            location = Attachment.FileLocation.remote,
                                            linkType = type.linkType
                                        ),
                                        title = attachment.title,
                                        key = attachment.key,
                                        libraryId = attachment.libraryId
                                    )
                                    return CustomResult.GeneralSuccess(newAttachment)
                                }
                            }
                        }

                    }
                }
            }

        } catch (e: Exception) {
            return CustomResult.GeneralError.CodeError(e)
        }
    }

}