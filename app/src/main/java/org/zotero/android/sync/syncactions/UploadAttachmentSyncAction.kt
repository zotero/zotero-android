package org.zotero.android.sync.syncactions

import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbRequest
import org.zotero.android.database.requests.CheckItemIsChangedDbRequest
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncActionError
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction
import org.zotero.android.sync.syncactions.data.AuthorizeUploadResponse
import timber.log.Timber
import java.io.File

class UploadAttachmentSyncAction(
    private val key: String,
    private val file: File,
    private val filename: String,
    private val md5: String,
    private val mtime: Long,
    private val libraryId: LibraryIdentifier,
    private val userId: Long,
    private val oldMd5: String?,
    var failedBeforeZoteroApiRequest: Boolean = true,
): SyncAction() {

    suspend fun result(): CustomResult<Unit> {
        try {
            when (this.libraryId) {
                is LibraryIdentifier.custom ->
                    //TODO implement WebDav
                   return zoteroResult()
                is LibraryIdentifier.group ->
                    return zoteroResult()
            }
        } catch (e :Exception) {
            return CustomResult.GeneralError.CodeError(e)
        }

    }

    fun createRequestBody(file: File): RequestBody {
        val mediaType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?.toMediaTypeOrNull()

        val requestBody = file.asRequestBody(mediaType)
        return requestBody
    }

    fun createPart(file: File, requestBody: RequestBody): MultipartBody.Part {
        return MultipartBody.Part.createFormData("file", file.name, requestBody)
    }

    private suspend fun zoteroResult(): CustomResult<Unit> {
        checkDatabase()
        val filesize = validateFile()
        this.failedBeforeZoteroApiRequest = false
        val authorizeUploadSyncActionNetworkResult = AuthorizeUploadSyncAction(
            key = this.key,
            filename = this.filename,
            filesize = filesize,
            md5 = this.md5,
            mtime = this.mtime,
            libraryId = this.libraryId,
            userId = this.userId,
            oldMd5 = this.oldMd5,
        ).result()

        if (authorizeUploadSyncActionNetworkResult !is CustomResult.GeneralSuccess) {
            processError(authorizeUploadSyncActionNetworkResult as CustomResult.GeneralError)
            return authorizeUploadSyncActionNetworkResult
        }
        val authorizedUploadResultValue = authorizeUploadSyncActionNetworkResult.value!!
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

                    val requestBody = createRequestBody(file)
                    val part = createPart(file, requestBody)

                    noAuthenticationApi.uploadAttachment(
                        url = authorizedUploadResultValue.authorizeNewUploadResponse.url,
                        headers = headers,
                        file = part,
                        params = authorizedUploadResultValue.authorizeNewUploadResponse.params
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
            val url =
                BuildConfig.BASE_API_URL + "/" + this.libraryId.apiPath(userId = this.userId) + "/items/" + this.key + "/file"
            syncApi.registerUpload(
                url = url,
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
            this.file.deleteRecursively()
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