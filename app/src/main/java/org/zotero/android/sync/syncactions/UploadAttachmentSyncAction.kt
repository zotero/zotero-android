package org.zotero.android.sync.syncactions

import android.webkit.MimeTypeMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.zotero.android.BuildConfig
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.CheckItemIsChangedDbRequest
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncActionError
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.data.AuthorizeUploadResponse
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.WebDavSessionStorage
import org.zotero.android.webdav.data.WebDavUploadResult
import timber.log.Timber
import java.io.File

class UploadAttachmentSyncAction @AssistedInject constructor(
    @Assisted("key") private val key: String,
    @Assisted("file") private val file: File,
    @Assisted("filename") private val filename: String,
    @Assisted("md5") private val md5: String,
    @Assisted("mtime") private val mtime: Long,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("userId") private val userId: Long,
    @Assisted("oldMd5") private val oldMd5: String?,
    @Assisted("failedBeforeZoteroApiRequest") var failedBeforeZoteroApiRequest: Boolean = true,

    private val authorizeUploadSyncActionFactory: AuthorizeUploadSyncAction.Factory,
    private val sessionStorage: WebDavSessionStorage,
    private val nonZoteroApi: NonZoteroApi,
    private val zoteroApi: ZoteroApi,
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val webDavController: WebDavController,
    private val submitUpdateSyncActionFactory: SubmitUpdateSyncAction.Factory
) {

    suspend fun result(): CustomResult<Unit> {
        return try {
            when (this.libraryId) {
                is LibraryIdentifier.custom ->
                    if (sessionStorage.isEnabled) {
                        webDavResult()
                    } else {
                        zoteroResult()
                    }

                is LibraryIdentifier.group ->
                    zoteroResult()
            }
        } catch (e: Exception) {
            CustomResult.GeneralError.CodeError(e)
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
        val authorizeUploadSyncActionNetworkResult = authorizeUploadSyncActionFactory.create(
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

                    nonZoteroApi.uploadAttachment(
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
            zoteroApi.registerUpload(
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

    private fun markAttachmentAsUploaded(version: Int?) {
        try {
            val requests: MutableList<DbRequest> = mutableListOf(
                MarkAttachmentUploadedDbRequest(
                    libraryId = this.libraryId,
                    key = this.key,
                    version = version
                )
            )
            if (version != null) {
                requests.add(
                    UpdateVersionsDbRequest(
                        version = version,
                        libraryId = this.libraryId,
                        type = UpdateVersionType.objectS(
                            SyncObject.item
                        )
                    )
                )
            }
            dbWrapperMain.realmDbStorage.perform(requests)
        } catch (error: Exception) {
            Timber.e("UploadAttachmentSyncAction: can't mark attachment as uploaded - $error")
            throw error
        }

    }

    private fun checkDatabase() {
        try {
            val request = CheckItemIsChangedDbRequest(libraryId = this.libraryId, key = this.key)
            val isChanged = dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
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
        val item = dbWrapperMain.realmDbStorage.perform(
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

    private suspend fun webDavResult(): CustomResult<Unit> {
        var file: File? = null
        var tUrl: String
        try {
            checkDatabase()
            validateFile()
            val prepareForUploadResult = webDavController.prepareForUpload(
                key = this.key,
                mtime = this.mtime,
                hash = this.md5,
                file = this.file
            )
            when (prepareForUploadResult) {
                WebDavUploadResult.exists -> {
                    Timber.d("UploadAttachmentSyncAction: file exists remotely")
                    markAttachmentAsUploaded(version = null)
                    throw SyncActionError.attachmentAlreadyUploaded
                }

                is WebDavUploadResult.new -> {
                    val url = prepareForUploadResult.url
                    val newFile = prepareForUploadResult.file
                    Timber.i("UploadAttachmentSyncAction: file needs upload")
                    file = newFile
                    webDavController.upload(url = url, file = file, key = this.key)
                    tUrl = url
                }
            }
        } catch (error: Exception) {
            val generalError = CustomResult.GeneralError.CodeError(error)
            if (file != null) {
                webDavController.finishUpload(key = this.key, result = generalError, file = file)
            }
            processError(generalError)
            return generalError
        }

        try {
            webDavController.finishUpload(
                key = this.key,
                result = CustomResult.GeneralSuccess(Triple(this.mtime, this.md5, tUrl)),
                file = file
            )
            val version: Int = submitItemWithHashAndMtime()
            markAttachmentAsUploaded(version)
        } catch (error: Exception) {
            val generalError = CustomResult.GeneralError.CodeError(error)
            processError(generalError)
            return generalError
        }

        return CustomResult.GeneralSuccess(Unit)
    }

    private suspend fun submitItemWithHashAndMtime(): Int {
        Timber.i("UploadAttachmentSyncAction: submit mtime and md5")
        val loadParameters: Map<String, Any>
        try {
            val item = dbWrapperMain.realmDbStorage.perform(
                request = ReadItemDbRequest(
                    libraryId = this.libraryId,
                    key = this.key
                )
            )
            val parameters = item.mtimeAndHashParameters
            item.realm?.refresh()
            loadParameters = parameters
        } catch (e: Exception) {
            Timber.e(e, "UploadAttachmentSyncAction: can't load params")
            throw e
        }
        this.failedBeforeZoteroApiRequest = false
        val submitUpdateSyncActionResult = submitUpdateSyncActionFactory.create(
            parameters = listOf(loadParameters),
            changeUuids = emptyMap(),
            sinceVersion = null,
            objectS = SyncObject.item,
            libraryId = this.libraryId,
            userId = this.userId,
            updateLibraryVersion = false,
        ).result()
        when (submitUpdateSyncActionResult) {
            is CustomResult.GeneralSuccess -> {
                val err = submitUpdateSyncActionResult.value?.second
                if (err != null) {
                    throw err.throwable
                }
                return submitUpdateSyncActionResult.value!!.first
            }
            is CustomResult.GeneralError.CodeError ->  {
                throw submitUpdateSyncActionResult.throwable
            }

            is CustomResult.GeneralError.NetworkError -> {
                throw Exception("Network Error. ${submitUpdateSyncActionResult.httpCode}, ${submitUpdateSyncActionResult.stringResponse}")
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("key") key: String,
            @Assisted("file") file: File,
            @Assisted("filename") filename: String,
            @Assisted("md5") md5: String,
            @Assisted("mtime") mtime: Long,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("userId") userId: Long,
            @Assisted("oldMd5") oldMd5: String?,
            @Assisted("failedBeforeZoteroApiRequest") failedBeforeZoteroApiRequest: Boolean = true,
        ): UploadAttachmentSyncAction
    }

}