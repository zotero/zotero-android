package org.zotero.android.sync.syncactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.data.AuthorizeUploadResponse
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction
import timber.log.Timber

class AuthorizeUploadSyncAction(
    val key: String,
    val filename: String,
    val filesize: Long,
    val md5: String,
    val mtime: Long,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val oldMd5: String?,
    val syncApi: SyncApi,
): SyncAction<CustomResult<AuthorizeUploadResponse>> {
    override suspend fun result(): CustomResult<AuthorizeUploadResponse> = withContext(Dispatchers.IO) {
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
                println(url)
                syncApi.authorizeUpload(
                    url = url,
                    headers = headers,
                    filename = this.filename,
                    filesize = this.filesize,
                    md5 = this.md5,
                    mtime = this.mtime,
                    params = 1
                )
            }


            if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
                return@run networkResult as CustomResult.GeneralError
            }

            try {
                val authorizeUploadResponse = AuthorizeUploadResponse.fromJson(networkResult.value!!, networkResult.lastModifiedVersion)
                return@run CustomResult.GeneralSuccess(authorizeUploadResponse)
            }catch (e : Exception) {
                Timber.e(e, "AuthorizeUploadSyncAction: can't authorize upload")
                Timber.e(e, "AuthorizeUploadSyncAction: key=${this.key};oldMd5=${this.oldMd5 ?: "null"};md5=${this.md5};filesize=${this.filesize};mtime=${this.mtime}")
                return@run CustomResult.GeneralError.CodeError(e)
            }
        }

    }
}