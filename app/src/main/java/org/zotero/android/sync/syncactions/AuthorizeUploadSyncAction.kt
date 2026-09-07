package org.zotero.android.sync.syncactions

import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncActionError

import org.zotero.android.sync.syncactions.data.AuthorizeUploadResponse
import timber.log.Timber

class AuthorizeUploadSyncAction @AssistedInject constructor(
    @Assisted("key") private val key: String,
    @Assisted("filename") private val filename: String,
    @Assisted("filesize") private val filesize: Long,
    @Assisted("md5") private val md5: String,
    @Assisted("mtime") private val mtime: Long,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("userId") private val userId: Long,
    @Assisted("oldMd5") private val oldMd5: String?,

    private val zoteroApi: ZoteroApi,
    private val gson: Gson,
) {
    suspend fun result(): CustomResult<AuthorizeUploadResponse> =
        withContext(Dispatchers.IO) {
            this@AuthorizeUploadSyncAction.run {
                val networkResult = safeApiCall {
                    val headers = mutableMapOf<String, String>()
                    val md5 = oldMd5
                    if (md5 != null) {
                        headers.put("If-Match", md5)
                    } else {
                        headers.put("If-None-Match", "*")
                    }
                    val url =
                        BuildConfig.BASE_API_URL + "/" + this.libraryId.apiPath(userId = this.userId) +
                                "/items/" + this.key + "/file"
                    zoteroApi.authorizeUpload(
                        url = url,
                        headers = headers,
                        filename = this.filename,
                        filesize = this.filesize,
                        md5 = this.md5,
                        mtime = this.mtime,
                        params = 1
                    )
                }

                try {
                    if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
                        val networkFailure = networkResult as CustomResult.GeneralError.NetworkError
                        val statusCode = networkFailure.httpCode
                        throw SyncActionError.authorizationFailed(
                            statusCode = statusCode,
                            response = networkFailure.stringResponse!!,
                            hadIfMatchHeader = (this.oldMd5 != null)
                        )
                    }
                    val authorizeUploadResponse = AuthorizeUploadResponse.fromJson(
                        data = networkResult.value!!,
                        lastModifiedVersion = networkResult.lastModifiedVersion,
                        gson = gson
                    )
                    return@run CustomResult.GeneralSuccess(authorizeUploadResponse)
                } catch (e: Exception) {
                    Timber.e(e, "AuthorizeUploadSyncAction: can't authorize upload")
                    Timber.e(
                        e,
                        "AuthorizeUploadSyncAction: key=${this.key};oldMd5=${this.oldMd5 ?: "null"};md5=${this.md5};filesize=${this.filesize};mtime=${this.mtime}"
                    )
                    return@run CustomResult.GeneralError.CodeError(e)
                }
            }

        }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("key") key: String,
            @Assisted("filename") filename: String,
            @Assisted("filesize") filesize: Long,
            @Assisted("md5") md5: String,
            @Assisted("mtime") mtime: Long,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("userId") userId: Long,
            @Assisted("oldMd5") oldMd5: String?
        ): AuthorizeUploadSyncAction
    }


}