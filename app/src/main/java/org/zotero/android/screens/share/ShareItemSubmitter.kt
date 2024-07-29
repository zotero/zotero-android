package org.zotero.android.screens.share

import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.backgrounduploader.BackgroundUpload
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.requests.CreateAttachmentDbRequest
import org.zotero.android.database.requests.CreateBackendItemDbRequest
import org.zotero.android.database.requests.CreateItemWithAttachmentDbRequest
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.database.requests.UpdateCollectionLastUsedDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.files.FileStore
import org.zotero.android.screens.share.backgroundprocessor.BackgroundUploadProcessor
import org.zotero.android.screens.share.data.CreateItemsResult
import org.zotero.android.screens.share.data.CreateResult
import org.zotero.android.screens.share.data.UploadData
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareSubmissionData
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.AuthorizeUploadSyncAction
import org.zotero.android.sync.syncactions.SubmitUpdateSyncAction
import org.zotero.android.sync.syncactions.data.AuthorizeUploadResponse
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.data.WebDavUploadResult
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareItemSubmitter @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
    private val fileStore: FileStore,
    private val backgroundUploadProcessor: BackgroundUploadProcessor,
    private val webDavController: WebDavController,
) {

    fun createItem(
        item: ItemResponse,
        libraryId: LibraryIdentifier,
        schemaController: SchemaController,
        dateParser: DateParser
    ): Pair<Map<String, Any>, Map<String, List<String>>> {
        var changeUuids: MutableMap<String, List<String>> = mutableMapOf()
        var parameters: MutableMap<String, Any> = mutableMapOf()
        dbWrapperMain.realmDbStorage.perform { coordinator ->
            val collectionKey = item.collectionKeys.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = libraryId
                    )
                )
            }

            val request = CreateBackendItemDbRequest(
                item = item,
                schemaController = schemaController,
                dateParser = dateParser
            )
            val item = coordinator.perform(request = request)
            parameters = item.updateParameters?.toMutableMap() ?: mutableMapOf()
            changeUuids = mutableMapOf(item.key to item.changes.map { it.identifier })

            coordinator.invalidate()
        }

        return parameters to changeUuids
    }

    private fun createItems(item: ItemResponse, attachment: Attachment): CreateItemsResult {
        Timber.i("ShareViewModel: create item and attachment db items")
        val parameters: MutableList<Map<String, Any>> = mutableListOf()
        var changeUuids: MutableMap<String, List<String>> = mutableMapOf()
        var mtime: Long? = null
        var md5: String? = null
        dbWrapperMain.realmDbStorage.perform { coordinator ->
            val collectionKey = item.collectionKeys.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = attachment.libraryId
                    )
                )
            }
            val request = CreateItemWithAttachmentDbRequest(
                item = item,
                attachment = attachment,
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                fileStore = this.fileStore
            )
            val (item, attachment) = coordinator.perform(request = request)
            val itemUpdateParameters = item.updateParameters
            if (itemUpdateParameters != null) {
                parameters.add(itemUpdateParameters)
            }
            val updateParameters = attachment.updateParameters
            if (updateParameters != null) {
                parameters.add(updateParameters)
            }
            changeUuids = mutableMapOf(item.key to item.changes.map { it.identifier },
                attachment.key to attachment.changes.map { it.identifier })

            mtime = attachment.fields.where().key(FieldKeys.Item.Attachment.mtime)
                .findFirst()?.value?.toLongOrNull()
            md5 = attachment.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst()?.value

            coordinator.invalidate()
        }
        if (mtime == null) {
            throw AttachmentState.Error.mtimeMissing
        }
        if (md5 == null) {
            throw AttachmentState.Error.md5Missing
        }
        return CreateItemsResult(parameters, changeUuids, md5!!, mtime!!)
    }

    fun create(
        attachment: Attachment,
        collections: Set<String>,
        tags: List<TagResponse>
    ): CreateResult {
        Timber.i("Create attachment db item")

        val localizedType =
            this.schemaController.localizedItemType(itemType = ItemTypes.attachment) ?: ""

        var updateParameters: Map<String, Any>? = null
        var changeUuids: Map<String, List<String>>? = null
        var md5: String? = null
        var mtime: Long? = null

        dbWrapperMain.realmDbStorage.perform { coordinator ->
            val collectionKey = collections.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = attachment.libraryId
                    )
                )
            }

            val request = CreateAttachmentDbRequest(
                attachment = attachment,
                parentKey = null,
                localizedType = localizedType,
                includeAccessDate = attachment.hasUrl,
                collections = collections,
                tags = tags,
                fileStore = fileStore
            )
            val attachment = coordinator.perform(request = request)

            updateParameters = attachment.updateParameters?.toMutableMap()
            changeUuids = mutableMapOf(attachment.key to attachment.changes.map { it.identifier })
            mtime = attachment.fields.where().key(FieldKeys.Item.Attachment.mtime)
                .findFirst()?.value?.toLongOrNull()
            md5 = attachment.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst()?.value

            coordinator.invalidate()
        }

        mtime ?: throw AttachmentState.Error.mtimeMissing
        md5 ?: throw AttachmentState.Error.md5Missing
        return CreateResult(
            updateParameters = updateParameters ?: emptyMap(),
            changeUuids = changeUuids ?: emptyMap(),
            md5 = md5!!,
            mtime = mtime!!
        )
    }

    private suspend fun prepareAndSubmit(
        attachment: Attachment,
        collections: Set<String>,
        tags: List<TagResponse>,
        file: File,
        tmpFile: File,
        libraryId: LibraryIdentifier,
        userId: Long,
    ): CustomResult<ShareSubmissionData> {
        val filesize = moveFile(tmpFile, file)
        val data: ShareSubmissionData
        val parameters: Map<String, Any>
        val changeUuids: Map<String, List<String>>
        try {
            val (params, uuids, md5, mtime) = create(
                attachment = attachment,
                collections = collections,
                tags = tags
            )
            parameters = params
            changeUuids = uuids
            data = ShareSubmissionData(filesize = filesize, md5 = md5, mtime = mtime)
        } catch (e: Exception) {
            file.delete()
            return CustomResult.GeneralError.CodeError(e)
        }
        val result = SubmitUpdateSyncAction(
            parameters = listOf(parameters),
            changeUuids = changeUuids,
            sinceVersion = null,
            objectS = SyncObject.item,
            libraryId = libraryId,
            userId = userId,
            updateLibraryVersion = false
        ).result()
        if (result is CustomResult.GeneralSuccess) {
            return CustomResult.GeneralSuccess(data)
        }
        return result as CustomResult.GeneralError
    }

    private suspend fun prepareAndSubmit(
        item: ItemResponse,
        attachment: Attachment,
        file: File,
        tmpFile: File,
        libraryId: LibraryIdentifier,
        userId: Long,
    ): CustomResult<ShareSubmissionData> {
        val filesize = moveFile(tmpFile, file)
        val data: ShareSubmissionData
        val parameters: List<Map<String, Any>>
        val changeUuids: Map<String, List<String>>
        try {
            val (params, uuids, md5, mtime) = createItems(item = item, attachment = attachment)
            parameters = params
            changeUuids = uuids
            data = ShareSubmissionData(filesize = filesize, md5 = md5, mtime = mtime)
        } catch (e: Exception) {
            file.delete()
            return CustomResult.GeneralError.CodeError(e)
        }

        val result = SubmitUpdateSyncAction(
            parameters = parameters,
            changeUuids = changeUuids,
            sinceVersion = null,
            objectS = SyncObject.item,
            libraryId = libraryId,
            userId = userId,
            updateLibraryVersion = false
        ).result()
        if (result is CustomResult.GeneralSuccess) {
            return CustomResult.GeneralSuccess(data)
        }
        return result as CustomResult.GeneralError
    }

    private suspend fun submit(data: UploadData): CustomResult<ShareSubmissionData> {
        when (val type = data.type) {
            is UploadData.Kind.file -> {
                val location = type.location
                val collections = type.collections
                val tags = type.tags
                Timber.i("ShareViewModel: prepare upload for local file")
                return prepareAndSubmit(
                    attachment = data.attachment,
                    collections = collections,
                    tags = tags,
                    file = data.file,
                    tmpFile = location,
                    libraryId = data.libraryId,
                    userId = data.userId,
                )
            }

            is UploadData.Kind.translated -> {
                val item = type.item
                val location = type.location
                Timber.i("ShareViewModel: prepare upload for local file")
                return prepareAndSubmit(
                    item = item,
                    attachment = data.attachment,
                    file = data.file,
                    tmpFile = location,
                    libraryId = data.libraryId,
                    userId = data.userId
                )
            }
        }
    }

    private fun moveFile(fromFile: File, toFile: File): Long {
        Timber.i("ShareViewModel: move file to attachment folder")

        try {
            val size = fromFile.length()
            if (size == 0L) {
                throw AttachmentState.Error.fileMissing
            }
            if (fromFile.renameTo(toFile)) {
                return size
            } else {
                throw AttachmentState.Error.fileMissing
            }
        } catch (error: Exception) {
            Timber.e(error, "ShareViewModel: can't move file")
            fromFile.delete()
            throw error
        }
    }

    suspend fun uploadToZotero(
        data: UploadData,
        attachmentKey: String,
        defaultMimetype: String,
        processUploadToZoteroException: (
            error: CustomResult.GeneralError,
            data: UploadData
        ) -> Unit,
        onBack: () -> Unit,
    ) {
        try {
            val submissionDataResult = submit(data = data)
            if (submissionDataResult is CustomResult.GeneralError) {
                processUploadToZoteroException(submissionDataResult, data)
                return
            }
            val submissionData = (submissionDataResult as CustomResult.GeneralSuccess).value!!
            val uploadSyncResult = AuthorizeUploadSyncAction(
                key = data.attachment.key,
                filename = data.filename,
                filesize = submissionData.filesize,
                md5 = submissionData.md5,
                mtime = submissionData.mtime,
                libraryId = data.libraryId,
                userId = data.userId,
                oldMd5 = null
            ).result()
            if (uploadSyncResult is CustomResult.GeneralError) {
                processUploadToZoteroException(uploadSyncResult, data)
                return
            }
            uploadSyncResult as CustomResult.GeneralSuccess
            val response = uploadSyncResult.value!!
            val md5 = submissionData.md5
            when (response) {
                is AuthorizeUploadResponse.exists -> {
                    Timber.i("ShareViewModel: file exists remotely")
                    val request = MarkAttachmentUploadedDbRequest(
                        libraryId = data.libraryId,
                        key = data.attachment.key,
                        version = response.version
                    )
                    dbWrapperMain.realmDbStorage.perform(request)
                }

                is AuthorizeUploadResponse.new -> {
                    val response = response.authorizeNewUploadResponse
                    Timber.i("ShareViewModel: upload authorized")

                    val upload = BackgroundUpload(
                        type = BackgroundUpload.Kind.zotero(uploadKey = response.uploadKey),
                        key = attachmentKey,
                        libraryId = data.libraryId,
                        userId = data.userId,
                        remoteUrl = response.url,
                        fileUrl = data.file,
                        md5 = md5,
                        date = Date()
                    )
                    backgroundUploadProcessor.startAsync(
                        upload = upload,
                        filename = data.filename,
                        mimeType = defaultMimetype,
                        parameters = response.params,
                        headers = mapOf("If-None-Match" to "*")
                    )

                }
            }
            onBack()
        } catch (e: Exception) {
            Timber.e(e, "Could not submit item or attachment")
            processUploadToZoteroException(CustomResult.GeneralError.CodeError(e), data)
        }
    }

    suspend fun uploadToWebDav(
        data: UploadData,
        attachmentKey: String,
        zipMimetype: String,
        processUploadToZoteroException: (
            error: CustomResult.GeneralError,
            data: UploadData
        ) -> Unit,
        onBack: () -> Unit,
    ) {
        try {
            val submissionDataResult = submit(data = data)
            if (submissionDataResult is CustomResult.GeneralError) {
                processUploadToZoteroException(submissionDataResult, data)
                return
            }
            val submissionData = (submissionDataResult as CustomResult.GeneralSuccess).value!!
            val response = webDavController.prepareForUpload(
                key = data.attachment.key,
                mtime = submissionData.mtime,
                hash = submissionData.md5,
                file = data.file
            )

            when (response) {
                is WebDavUploadResult.exists -> {
                    Timber.i("ShareViewModel: file exists remotely")
                    val request = MarkAttachmentUploadedDbRequest(
                        libraryId = data.libraryId,
                        key = data.attachment.key,
                        version = null
                    )
                    dbWrapperMain.realmDbStorage.perform(request)
                }

                is WebDavUploadResult.new -> {
                    val url = response.url
                    val file = response.file
                    Timber.i("ShareViewModel: upload authorized")

                    val upload = BackgroundUpload(
                        type = BackgroundUpload.Kind.webdav(submissionData.mtime),
                        key = attachmentKey,
                        libraryId = data.libraryId,
                        userId = data.userId,
                        remoteUrl = url,
                        fileUrl = file,
                        md5 = submissionData.md5,
                        date = Date()
                    )
                    backgroundUploadProcessor.startAsync(
                        upload = upload,
                        filename = data.attachment.key + ".zip",
                        mimeType = zipMimetype,
                        parameters = LinkedHashMap(),
                        headers = emptyMap()
                    )

                }
            }
            onBack()
        } catch (e: Exception) {
            Timber.e(e, "Could not submit item or attachment to webdav")
            processUploadToZoteroException(CustomResult.GeneralError.CodeError(e), data)
        }

    }
}