package org.zotero.android.webdav

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.zotero.android.api.WebDavApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.StoreMtimeForAttachmentDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.data.WebDavError
import timber.log.Timber
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WebDavController @Inject constructor(
    private val sessionStorage: WebDavSessionStorage,
    private val dbWrapper: DbWrapper,
    private val webDavApi: WebDavApi,
    private val gson: Gson,
) {

    private fun update(mtime: Long, key: String) {
        try {
            dbWrapper.realmDbStorage.perform(
                StoreMtimeForAttachmentDbRequest(
                    mtime = mtime,
                    key = key,
                    libraryId = LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            )
        } catch (error: Exception) {
            Timber.e(error, "WebDavController: can't update mtime")
            throw error
        }
    }

    private fun remove(file: File) {
        file.delete()
    }

    val currentUrl: String?
        get() {
            val createUrlResult = _createUrl()
            if (createUrlResult is CustomResult.GeneralSuccess) {
                return createUrlResult.value
            }
            //failing silently
            return null
        }

    private fun createUrl(): CustomResult<String> {
        val url = _createUrl()
        return url

    }

    private fun _createUrl(): CustomResult<String> {
        val url = sessionStorage.url
        if (url.isEmpty()) {
            Timber.e("WebDavController: url not found")
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.noUrl)
        }

        val urlComponents = url.split("/")
        if (urlComponents.isEmpty()) {
            Timber.e("WebDavController: url components empty - $url")
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.invalidUrl)
        }

        val hostComponents = (urlComponents.firstOrNull() ?: "").split(":")
        val host = hostComponents.firstOrNull()
        val port = hostComponents.lastOrNull()?.toIntOrNull()

        val path: String
        if (urlComponents.size == 1) {
            path = "/zotero/"
        } else {
            path = "/" + urlComponents.drop(1).filter { it.isNotEmpty() }
                .joinToString(separator = "/") + "/zotero/"
        }

        try {
            val components = URL(sessionStorage.scheme.name, host, port!!, path)
            return CustomResult.GeneralSuccess(components.toExternalForm())
        } catch (e: Exception) {
            Timber.e("WebDavController: could not create url from components. url=$url; host=${host ?: "missing"}; path=$path; port=${port?.toString() ?: "missing"}")
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.invalidUrl)
        }

    }

    private fun loadCredentials(): CustomResult<Pair<String, String>> {
        val username = sessionStorage.username
        if (username.isEmpty()) {
            Timber.e("WebDavController: username not found")
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.noUsername)
        }
        val password = sessionStorage.password
        if (password.isEmpty()) {
            Timber.e("WebDavController: password not found")
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.noPassword)
        }
        return CustomResult.GeneralSuccess(username to password)
    }

    fun resetVerification() {
        this.sessionStorage.isVerified = false
    }

    suspend fun checkServer(): CustomResult<String> {
        Timber.i("WebDavController: checkServer")
        val loadCredsResult = loadCredentials()
        if (loadCredsResult is CustomResult.GeneralError) {
            processCheckServerError(loadCredsResult)
            return loadCredsResult
        }

        val createUrlResult = createUrl()
        if (createUrlResult is CustomResult.GeneralError) {
            processCheckServerError(createUrlResult)
            return createUrlResult
        }
        createUrlResult as CustomResult.GeneralSuccess
        val url = createUrlResult.value!!

        val checkIsDavResult = checkIsDav(url = url)
        if (checkIsDavResult is CustomResult.GeneralError) {
            processCheckServerError(checkIsDavResult)
            return checkIsDavResult
        }

        val checkZoteroDirectoryResult = checkZoteroDirectory(url)
        if (checkZoteroDirectoryResult is CustomResult.GeneralError) {
            processCheckServerError(checkZoteroDirectoryResult)
            return checkZoteroDirectoryResult
        }
        sessionStorage.isVerified = true
        Timber.i("WebDavController: file sync is successfully set up")

        return CustomResult.GeneralSuccess(url)
    }

    private fun processCheckServerError(error: CustomResult.GeneralError) {
        if (error is CustomResult.GeneralError.NetworkError) {
            Timber.e("WebDavController: checkServer failed:${error.stringResponse}")
        }
        if (error is CustomResult.GeneralError.CodeError) {
            Timber.e(error.throwable, "WebDavController: checkServer failed")
        }

        if ((error as? CustomResult.GeneralError.CodeError)?.throwable as? WebDavError.Verification is WebDavError.Verification.fileMissingAfterUpload) {
            sessionStorage.isVerified = true
        }
    }

    private suspend fun checkIsDav(url: String): CustomResult<ResponseBody> {
        val networkResult = safeApiCall {
            webDavApi.options(url)
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        if (!listOf(200, 204, 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        val headers = networkResult.headers
        if (!headers.any { it.first.contains("dav", ignoreCase = true) }) {
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.notDav)
        }
        return networkResult
    }

    suspend fun createZoteroDirectory():  CustomResult<ResponseBody> {
        val createUrlResult = createUrl()
        if (createUrlResult is CustomResult.GeneralError) {
            return createUrlResult
        }
        createUrlResult as CustomResult.GeneralSuccess
        val url = createUrlResult.value!!
        val networkResult = safeApiCall {
            webDavApi.mkcol(url)
        }

        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        if (!listOf(200, 201, 204).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        return networkResult

    }

    private suspend fun checkZoteroDirectory(url: String): CustomResult<ResponseBody> {
        val propFindResult = propFind(url = url)
        if (propFindResult is CustomResult.GeneralSuccess.NetworkSuccess && propFindResult.httpCode == 207) {
            val checkWhetherReturns404ForMissingFileResult =
                checkWhetherReturns404ForMissingFile(url = url)
            if (checkWhetherReturns404ForMissingFileResult !is CustomResult.GeneralSuccess) {
                return checkWhetherReturns404ForMissingFileResult as CustomResult.GeneralError
            }
            val checkWritabilityResult = checkWritability(url = url)
            if (checkWritabilityResult !is CustomResult.GeneralSuccess) {
                return checkWritabilityResult as CustomResult.GeneralError
            }
            return checkWritabilityResult
        }
        if (propFindResult is CustomResult.GeneralError.NetworkError && propFindResult.httpCode == 404) {
            return checkWhetherParentAvailable(url = url)
        }

        return propFindResult
    }

    private suspend fun propFind(url: String): CustomResult<ResponseBody> {
        val headers = mapOf("Content-Type" to "text/xml; charset=utf-8", "Depth" to "0")
        val bodyText = "<propfind xmlns='DAV:'><prop><getcontentlength/></prop></propfind>"

        val networkResult = safeApiCall {
            val body: RequestBody = bodyText.toRequestBody()
            webDavApi.propfind(url = url, headers = headers, body = body)
        }
        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        if (!listOf(207, 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        return networkResult
    }

    private suspend fun checkWhetherReturns404ForMissingFile(url: String): CustomResult<Unit> {
        val appendedUrl = "$url/nonexistent.prop"
        val networkResult = safeApiCall {
            webDavApi.get(url = appendedUrl)
        }
        if (networkResult is CustomResult.GeneralError.NetworkError) {
            if (networkResult.httpCode == 404) {
                return CustomResult.GeneralSuccess(Unit)
            }
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!((200..<300).toList() + 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        } else {
            return CustomResult.GeneralError.CodeError(WebDavError.Verification.nonExistentFileNotMissing)
        }
    }

    private suspend fun checkWritability(url: String): CustomResult<ResponseBody> {
        val appendedUrl = "${url}zotero-test-file.prop"
        val webDavTestWriteRequestResult = webDavTestWriteRequest(appendedUrl)
        if (webDavTestWriteRequestResult !is CustomResult.GeneralSuccess) {
            return webDavTestWriteRequestResult as CustomResult.GeneralError
        }

        val webDavDownloadRequestResult = webDavDownloadRequest(appendedUrl)
        if (webDavDownloadRequestResult !is CustomResult.GeneralSuccess) {
            return webDavDownloadRequestResult as CustomResult.GeneralError
        }

        val webDavDeleteRequestResult = webDavDeleteRequest(appendedUrl)
        if (webDavDeleteRequestResult !is CustomResult.GeneralSuccess) {
            return webDavDeleteRequestResult as CustomResult.GeneralError
        }
        return webDavDeleteRequestResult
    }

    private suspend fun webDavDeleteRequest(url: String): CustomResult<ResponseBody> {
        val networkResult = safeApiCall {
            webDavApi.delete(url = url)
        }
        if (networkResult is CustomResult.GeneralError.NetworkError) {
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 204, 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        return networkResult
    }

    private suspend fun webDavDownloadRequest(url: String): CustomResult<ResponseBody> {
        val networkResult = safeApiCall {
            webDavApi.get(url = url)
        }
        if (networkResult is CustomResult.GeneralError.NetworkError) {
            if (networkResult.httpCode == 404) {
                return CustomResult.GeneralError.CodeError(WebDavError.Verification.fileMissingAfterUpload)
            }
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        return networkResult
    }

    private suspend fun webDavTestWriteRequest(url: String): CustomResult<ResponseBody> {
        val bodyText = " "

        val networkResult = safeApiCall {
            val body: RequestBody = RequestBody.create("text/plain".toMediaType(), bodyText);
            webDavApi.put(url = url, body = body)
        }

        if (networkResult is CustomResult.GeneralError.NetworkError) {
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 201, 204).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }

        return networkResult
    }

    private suspend fun checkWhetherParentAvailable(url: String): CustomResult<ResponseBody> {
        val lastSlashPosition = url.lastIndexOf(char = '/', startIndex = url.length - 2)
        val newUrl = url.substring(0, lastSlashPosition + 1)
        val propFindResult = propFind(newUrl)
        if (propFindResult is CustomResult.GeneralError) {
            return propFindResult
        }

        if (propFindResult is CustomResult.GeneralSuccess.NetworkSuccess) {
            if (propFindResult.httpCode == 207) {
                return CustomResult.GeneralError.CodeError(
                    WebDavError.Verification.zoteroDirNotFound(
                        url
                    )
                )
            } else {
                return CustomResult.GeneralError.CodeError(WebDavError.Verification.parentDirNotFound)
            }
        }
        return propFindResult
    }


}