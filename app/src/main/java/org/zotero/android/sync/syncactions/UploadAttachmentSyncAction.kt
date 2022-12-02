package org.zotero.android.sync.syncactions

import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.CheckItemIsChangedDbRequest
import org.zotero.android.architecture.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.architecture.database.requests.ReadItemDbRequest
import org.zotero.android.architecture.database.requests.UpdateVersionType
import org.zotero.android.architecture.database.requests.UpdateVersionsDbRequest
import org.zotero.android.data.AuthorizeUploadResponse
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncActionError
import org.zotero.android.sync.SyncActionWithError
import org.zotero.android.sync.SyncObject
import timber.log.Timber
import java.io.File

class UploadAttachmentSyncAction(
    val key: String,
    val file: File,
    val filename: String,
    val md5: String,
    val mtime: Long,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val oldMd5: String?,
    var failedBeforeZoteroApiRequest: Boolean = true,

    val syncApi: SyncApi,
    val dbWrapper: DbWrapper,
    val fileStore: FileStore,
    val schemaController: SchemaController
): SyncActionWithError<Unit> {

    override suspend fun result(): CustomResult<Unit> {
        try {
            when (this.libraryId) {
                is LibraryIdentifier.custom ->
                    throw RuntimeException("Not implemented yet")
                is LibraryIdentifier.group ->
                    return zoteroResult()
            }
        } catch (e :Exception) {
            return CustomResult.GeneralError.CodeError(e)
        }

    }

    private suspend fun zoteroResult(): CustomResult<Unit> {
        checkDatabase()
        val filesize = validateFile()
        this.failedBeforeZoteroApiRequest = false
        val authorizeUploadSyncActionNetworkResult = AuthorizeUploadSyncAction(key = this.key, filename = this.filename,
            filesize = filesize, md5 = this.md5, mtime = this.mtime, libraryId = this.libraryId,
            userId = this.userId, oldMd5 = this.oldMd5, syncApi = this.syncApi).result()

        if (authorizeUploadSyncActionNetworkResult !is CustomResult.GeneralSuccess) {
            processError(authorizeUploadSyncActionNetworkResult as CustomResult.GeneralError)
            return authorizeUploadSyncActionNetworkResult
        }
        val authorizedUploadResultValue = authorizeUploadSyncActionNetworkResult.value
        val uploadKey: String
        when(authorizedUploadResultValue) {
            is AuthorizeUploadResponse.exists -> {
                Timber.d("UploadAttachmentSyncAction: file exists remotely")
                markAttachmentAsUploaded(version = authorizedUploadResultValue.version)
                throw SyncActionError.attachmentAlreadyUploaded
            }
            is AuthorizeUploadResponse.new -> {
                Timber.d("UploadAttachmentSyncAction: file needs upload")

                val uploadAttachmentNetworkResult = safeApiCall {
                    val headers = mutableMapOf<String, String>()
                    headers.put("If-None-Match", "*")

                    for (entry in authorizedUploadResultValue.authorizeNewUploadResponse.params) {
                        headers.put(entry.key, entry.value)
                    }

                    val mediaType =
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                            ?.toMediaTypeOrNull()

                    val requestBody = file.asRequestBody(mediaType)

                    syncApi.uploadAttachment(
                        url = authorizedUploadResultValue.authorizeNewUploadResponse.url,
                        headers = headers,
                        requestBody =requestBody,
                    )
                }

                if (uploadAttachmentNetworkResult !is CustomResult.GeneralSuccess) {
                    processError(uploadAttachmentNetworkResult as CustomResult.GeneralError)
                    return uploadAttachmentNetworkResult
                }

                uploadKey = authorizedUploadResultValue.authorizeNewUploadResponse.uploadKey
            }
        }

        val registerUploadNetworkResult = safeApiCall {

            val headers = mutableMapOf<String, String>()
            val md5 = oldMd5
            if (md5 != null) {
                headers.put("If-Match", md5)
            } else {
                headers.put("If-None-Match", "*")
            }

            syncApi.registerUpload(
                basePath = this.libraryId.apiPath(userId = this.userId),
                key = this.key,
                headers = headers,
                upload = uploadKey
            )
        }
        if (registerUploadNetworkResult !is CustomResult.GeneralSuccess) {
            processError(registerUploadNetworkResult as CustomResult.GeneralError)
            return registerUploadNetworkResult
        }
        registerUploadNetworkResult as CustomResult.GeneralSuccess.NetworkSuccess
        val lastModifiedVersion = registerUploadNetworkResult.lastModifiedVersion
        markAttachmentAsUploaded(lastModifiedVersion)
        return CustomResult.GeneralSuccess(Unit)
    }

    private fun processError(generalError: CustomResult.GeneralError) {
        val codeError = generalError as? CustomResult.GeneralError.CodeError
        val error = codeError?.throwable as? SyncActionError
        if (error != null) {
            when (error) {
                SyncActionError.attachmentAlreadyUploaded -> {
                    return
                }
                else -> {
                    //no-op
                }
            }
        }
        Timber.e(codeError?.throwable, "UploadAttachmentSyncAction: could not upload")

    }

    private suspend fun markAttachmentAsUploaded(version: Int?) {
        try {
            var requests: MutableList<DbRequest> = mutableListOf( MarkAttachmentUploadedDbRequest(libraryId = this.libraryId, key = this.key, version = version))
            if (version != null) {
                requests.add(UpdateVersionsDbRequest(version = version, libraryId =this.libraryId, type=  UpdateVersionType.objectS(
                    SyncObject.item)))
            }
            dbWrapper.realmDbStorage.perform(requests)
        }catch (error: Exception) {
            Timber.e("UploadAttachmentSyncAction: can't mark attachment as uploaded - $error")
            throw error
        }

    }

    private fun checkDatabase() {
        try {
            val request = CheckItemIsChangedDbRequest(libraryId = this.libraryId, key = this.key)
            val isChanged = dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
            if (!isChanged) {
                return
            } else {
                Timber.e("UploadAttachmentSyncAction: attachment item not submitted")
                throw SyncActionError.attachmentItemNotSubmitted
            }
        } catch (e: Exception) {
            Timber.e("UploadAttachmentSyncAction: could not check item submitted - $e")
            throw e
        }
    }

    private fun validateFile(): Long {
        val size = this.file.length()

        if (size > 0) {
            if (this.file.extension != "pdf" || fileStore.isPdf(file = this.file)) {
                return size
            }
            this.file.delete()
        }

        Timber.e("UploadAttachmentSyncAction: missing attachment - ${this.file.absolutePath}")
        val item = dbWrapper.realmDbStorage.perform(
            ReadItemDbRequest(
                libraryId = this.libraryId,
                key = this.key
            )
        )
        val title = item.displayTitle
        //TODO invalidate realm
        throw SyncActionError.attachmentMissing(
            key = this.key,
            libraryId = this.libraryId,
            title = title
        )

    }


}