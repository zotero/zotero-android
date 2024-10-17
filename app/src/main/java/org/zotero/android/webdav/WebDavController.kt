package org.zotero.android.webdav

import android.webkit.MimeTypeMap
import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DefaultRequestCacheKeyProvider
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.api.WebDavApi
import org.zotero.android.api.interceptors.UserAgentHeaderNetworkInterceptor
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.network.safeApiCallSync
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.StoreMtimeForAttachmentDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.Zipper
import org.zotero.android.ktx.setNetworkTimeout
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.data.MetadataResult
import org.zotero.android.webdav.data.WebDavDeletionResult
import org.zotero.android.webdav.data.WebDavError
import org.zotero.android.webdav.data.WebDavUploadResult
import retrofit2.Response
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume


@Singleton
class WebDavController @Inject constructor(
    private val sessionStorage: WebDavSessionStorage,
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val userAgentHeaderNetworkInterceptor: UserAgentHeaderNetworkInterceptor,
) {

    private fun update(mtime: Long, key: String): CustomResult<Unit> {
        try {
            dbWrapperMain.realmDbStorage.perform(
                StoreMtimeForAttachmentDbRequest(
                    mtime = mtime,
                    key = key,
                    libraryId = LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            )
            return CustomResult.GeneralSuccess(Unit)
        } catch (error: Exception) {
            Timber.e(error, "WebDavController: can't update mtime")
            return CustomResult.GeneralError.CodeError(error)
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
            val components = if (port != null) {
                URL(sessionStorage.scheme.name, host, port, path)
            } else {
                URL(sessionStorage.scheme.name, host, path)
            }
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
            provideWebDavApi().options(url)
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
            provideWebDavApi().mkcol(url)
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
            provideWebDavApi().propfind(url = url, headers = headers, body = body)
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
        val appendedUrl = "${url}nonexistent.prop"
        val networkResult = safeApiCall {
            provideWebDavApi().get(url = appendedUrl)
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
            provideWebDavApi().delete(url = url)
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
            provideWebDavApi().get(url = url)
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
            provideWebDavApi().put(url = url, body = body)
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

    suspend fun download(key: String): CustomResult<ResponseBody> {
        val checkServerIfNeededResult = checkServerIfNeeded()
        if (checkServerIfNeededResult is CustomResult.GeneralError) {
            return checkServerIfNeededResult
        }
        checkServerIfNeededResult as CustomResult.GeneralSuccess
        val newUrl = "${checkServerIfNeededResult.value}$key.zip"
        return safeApiCall {
            provideWebDavApi().downloadFile(newUrl)
        }
    }

    private suspend fun checkServerIfNeeded(): CustomResult<String> {
        if (sessionStorage.isVerified) {
            return createUrl()
        }
        val checkServerResult = checkServer()
        if (checkServerResult is CustomResult.GeneralError) {
            if ((checkServerResult as? CustomResult.GeneralError.CodeError)?.throwable as? WebDavError.Verification is WebDavError.Verification.fileMissingAfterUpload) {
                //no-op
            } else {
                return checkServerResult
            }

            val createUrlResult = _createUrl()
            if (createUrlResult is CustomResult.GeneralError) {
                return createUrlResult
            }
            createUrlResult as CustomResult.GeneralSuccess
            val url = createUrlResult.value!!
            CustomResult.GeneralSuccess(url)
        }
        return checkServerResult
    }

    private suspend fun checkMetadata(key: String, mtime: Long, hash: String, url: String): CustomResult<MetadataResult> {
        Timber.i("WebDavController: check metadata for $key")
        val metadataResult = metadata(key = key, url = url)
        if (metadataResult is CustomResult.GeneralError) {
            return metadataResult
        }
        metadataResult as CustomResult.GeneralSuccess
        val metadataResultValue = metadataResult.value
        if (metadataResultValue == null) {
            return CustomResult.GeneralSuccess(MetadataResult.new(url))
        }
        val (remoteMtime, remoteHash) = metadataResultValue
        if (hash == remoteHash) {
            return CustomResult.GeneralSuccess(
                if (mtime == remoteMtime) {
                    MetadataResult.unchanged
                } else {
                    MetadataResult.mtimeChanged(remoteMtime)
                }
            )
        } else {
            return CustomResult.GeneralSuccess(MetadataResult.changed(url))
        }
    }

    private suspend fun metadata(key: String, url: String): CustomResult<Pair<Long, String>?> {
        val newUrl = "${url}${key}.prop"
        val networkResult = safeApiCall {
            provideWebDavApi().get(url = newUrl)
        }
        if (networkResult is CustomResult.GeneralError.NetworkError) {
            if (networkResult.httpCode == 404) {
                return CustomResult.GeneralSuccess(null)
            }
            Timber.e("WebDavController: $key item prop file error: ${networkResult.stringResponse}")
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 404).contains(networkResult.httpCode)) {
            return CustomResult.GeneralError.UnacceptableStatusCode(networkResult.httpCode, null)
        }
        val propContents = networkResult.value!!.string()
        try {
            val (mtime, hash) = MTimeAndHashXmlParser.readMtimeAndHash(propContents)
            return CustomResult.GeneralSuccess(mtime to hash)
        } catch (e: Exception) {
            Timber.e(e, "WebDavController: $key item prop invalid. input = $propContents")
            return CustomResult.GeneralError.CodeError(
                WebDavError.Download.itemPropInvalid(
                    propContents
                ))
        }
    }

    private suspend fun removeExistingMetadata(
        key: String,
        url: String
    ): CustomResult<ResponseBody> {
        Timber.i("WebDavController: remove metadata for $key")

        val newUrl = "${url}${key}.prop"

        val networkResult = safeApiCall {
            provideWebDavApi().delete(url = newUrl)
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

    private fun zip(file: File, key: String): CustomResult<File> {
        Timber.i("WebDavController: ZIP file for upload")
        try {
            val tmpFile = fileStore.temporaryZipUploadFile(key = key)
            tmpFile.delete()
            Zipper.zip(files = listOf(file), zipFile = tmpFile)
            return CustomResult.GeneralSuccess(tmpFile)
        } catch (error: Exception) {
            Timber.e(error, "WebDavController: can't zip file")
            return CustomResult.GeneralError.CodeError(error)
        }
    }

    suspend fun prepareForUpload(
        key: String,
        mtime: Long,
        hash: String,
        file: File
    ): WebDavUploadResult {
        Timber.i("WebDavController: prepare for upload")

        val checkServerIfNeededResult = checkServerIfNeeded()
        if (checkServerIfNeededResult is CustomResult.GeneralError) {
            handlePrepareForUploadError(checkServerIfNeededResult)
        }
        checkServerIfNeededResult as CustomResult.GeneralSuccess
        val url = checkServerIfNeededResult.value!!

        val checkMetadataResult = checkMetadata(key = key, mtime = mtime, hash = hash, url = url)
        if (checkMetadataResult is CustomResult.GeneralError) {
            handlePrepareForUploadError(checkMetadataResult)
        }
        checkMetadataResult as CustomResult.GeneralSuccess
        val result = checkMetadataResult.value!!
        when (result) {
            MetadataResult.unchanged -> {
                return WebDavUploadResult.exists
            }

            is MetadataResult.new -> {
                val url = result.url

                val zipResult = zip(file = file, key = key)
                if (zipResult is CustomResult.GeneralError) {
                    handlePrepareForUploadError(zipResult)
                }
                zipResult as CustomResult.GeneralSuccess
                return WebDavUploadResult.new(url, zipResult.value!!)
            }

            is MetadataResult.mtimeChanged -> {
                val mtime = result.mtime
                val updateResult = update(mtime = mtime, key = key)
                if (updateResult is CustomResult.GeneralError) {
                    handlePrepareForUploadError(updateResult)
                }
                return WebDavUploadResult.exists
            }

            is MetadataResult.changed -> {
                val url = result.url
                val removeExistingMetadataResult = removeExistingMetadata(key = key, url = url)
                if (removeExistingMetadataResult is CustomResult.GeneralError) {
                    handlePrepareForUploadError(removeExistingMetadataResult)
                }
                val zipResult = zip(file = file, key = key)
                if (zipResult is CustomResult.GeneralError) {
                    handlePrepareForUploadError(zipResult)
                }
                zipResult as CustomResult.GeneralSuccess
                return WebDavUploadResult.new(url, zipResult.value!!)
            }
        }
    }

    private fun handlePrepareForUploadError(error: CustomResult.GeneralError) {
        when (error) {
            is CustomResult.GeneralError.CodeError -> {
                throw error.throwable
            }

            is CustomResult.GeneralError.NetworkError -> {
                throw WebDavError.Upload.apiError(error = error, httpMethod = null)
            }
        }

    }

    suspend fun finishUpload(
        key: String,
        result: CustomResult<Triple<Long, String, String>>,
        file: File?
    ) {
        when (result) {
            is CustomResult.GeneralSuccess -> {
                val mtime = result.value!!.first
                val hash = result.value!!.second
                val url = result.value!!.third
                uploadMetadata(key = key, mtime = mtime, hash = hash, url = url)
                if (file != null) {
                    remove(file)
                }
            }

            is CustomResult.GeneralError -> {
                when (result) {
                    is CustomResult.GeneralError.CodeError -> {
                        Timber.e(result.throwable, "WebDavController: finish failed upload")
                    }

                    is CustomResult.GeneralError.NetworkError -> {
                        Timber.e("WebDavController: finish failed upload - ${result.stringResponse}")
                    }
                }
                if (file != null) {
                    remove(file)
                }
            }
        }
    }

    private suspend fun uploadMetadata(key: String, mtime: Long, hash: String, url: String) {
        Timber.i("WebDavController: upload metadata for $key")
        val metadataProp = "<properties version=\"1\"><mtime>$mtime</mtime><hash>$hash</hash></properties>"
        val data = metadataProp.toRequestBody()
        val newUrl = "${url}${key}.prop"

        val networkResult = safeApiCall {
            provideWebDavApi().uploadProp(url = newUrl, body = data)
        }

        if (networkResult is CustomResult.GeneralError.NetworkError) {
            throw WebDavError.Upload.apiError(error = networkResult, httpMethod = "PUT")
        }
        if (networkResult is CustomResult.GeneralError.CodeError) {
            throw networkResult.throwable
        }
    }

    suspend fun upload(key: String, url: String, file: File) {
        val newUrl = "${url}${key}.zip"

        val requestBody = createRequestBody(file)

        val networkResult = safeApiCall {
            provideWebDavApi().uploadAttachment(url = newUrl, body = requestBody)
        }

        if (networkResult is CustomResult.GeneralError.NetworkError) {
            throw WebDavError.Upload.apiError(error = networkResult, httpMethod = "PUT")
        }
        if (networkResult is CustomResult.GeneralError.CodeError) {
            throw networkResult.throwable
        }

    }

    private fun createRequestBody(file: File): RequestBody {
        val mediaType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
                ?.toMediaTypeOrNull()

        val requestBody = file.asRequestBody(mediaType)
        return requestBody
    }

    suspend fun delete(keys: List<String>): CustomResult<WebDavDeletionResult> {
        val checkServerIfNeededResult = checkServerIfNeeded()
        if (checkServerIfNeededResult is CustomResult.GeneralError) {
            return checkServerIfNeededResult
        }
        checkServerIfNeededResult as CustomResult.GeneralSuccess
        val url = checkServerIfNeededResult.value!!
        return performDeletions(url = url, keys = keys)
    }

    private suspend fun performDeletions(
        url: String,
        keys: List<String>
    ): CustomResult<WebDavDeletionResult> {
        return suspendCancellableCoroutine { cont ->

            var count = keys.size * 2
            val succeeded = mutableSetOf<String>()
            val missing= mutableSetOf<String>()
            val failed= mutableSetOf<String>()

            val processResult: (String, CustomResult<ResponseBody>) -> Unit = { key, result ->
                when(result) {
                    is CustomResult.GeneralSuccess -> {
                        if (!failed.contains(key) && !missing.contains(key)) {
                            succeeded.add(key)
                        }
                    }

                    is CustomResult.GeneralError -> {
                        succeeded.remove(key)
                        missing.remove(key)
                        failed.add(key)
                    }
                }


                count -= 1

                if (count == 0) {
                    cont.resume(
                        CustomResult.GeneralSuccess(
                            WebDavDeletionResult(
                                succeeded = succeeded,
                                missing = missing,
                                failed = failed
                            )
                        )
                    )
                }
            }

            for (key in keys) {
                performDeletionsProp(url, key, processResult)
                performDeletionsZip(url, key, processResult)
            }
        }
    }

    private fun performDeletionsZip(
        url: String,
        key: String,
        processResult: (String, CustomResult<ResponseBody>) -> Unit
    ) {

        val zipUrl = "${url}${key}.zip"
        val deleteZipResult = safeApiCallSync {
            provideWebDavApi().deleteSync(url = zipUrl)
        }
        if (deleteZipResult is CustomResult.GeneralError) {
            processResult(key, deleteZipResult)
            return
        }
        deleteZipResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 204, 404).contains(deleteZipResult.httpCode)) {
            processResult(key, deleteZipResult)
            return
        }
        processResult(key, deleteZipResult)
    }

    private fun performDeletionsProp(
        url: String,
        key: String,
        processResult: (String, CustomResult<ResponseBody>) -> Unit
    ) {
        val propUrl = "${url}${key}.prop"
        val deletePropResult = safeApiCallSync {
            provideWebDavApi().deleteSync(url = propUrl)
        }
        if (deletePropResult is CustomResult.GeneralError) {
            processResult(key, deletePropResult)
            return
        }
        deletePropResult as CustomResult.GeneralSuccess.NetworkSuccess
        if (!listOf(200, 204, 404).contains(deletePropResult.httpCode)) {
            processResult(key, deletePropResult)
            return
        }
        processResult(key, deletePropResult)
    }

    suspend fun uploadAttachment(url: String, body: RequestBody): Response<Unit> {
        return provideWebDavApi().uploadAttachment(url, body)
    }

    private val authCache: Map<String, CachingAuthenticator> =
        ConcurrentHashMap<String, CachingAuthenticator>()

    private fun provideWebDavOkHttpClient(
    ): OkHttpClient {
        val connectionPool = ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = 30
        dispatcher.maxRequestsPerHost = 30

        val username = sessionStorage.username
        val password = sessionStorage.password

        val credentials = Credentials(username, password)
        val basicAuthenticator = BasicAuthenticator(credentials)
        val digestAuthenticator = DigestAuthenticator(credentials)
        val authenticator = DispatchingAuthenticator.Builder()
            .with("digest", digestAuthenticator)
            .with("basic", basicAuthenticator)
            .build()

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .setNetworkTimeout(15L)
            .authenticator(CachingAuthenticatorDecorator(authenticator, authCache))
            .addInterceptor(
                AuthenticationCacheInterceptor(
                    authCache,
                    DefaultRequestCacheKeyProvider()
                )
            )
            .addInterceptor(userAgentHeaderNetworkInterceptor)
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor { message ->
                if (message.contains("Authorization")) {
                    Timber.d("Authorization: redacted")
                } else {
                    Timber.d(message)
                }
            }.apply { level = Level.BODY })
            .build()
    }

    private fun provideWebDavRetrofit(
    ): Retrofit {
        val okHttpClient = provideWebDavOkHttpClient()
        val retrofitBuilder = Retrofit.Builder()
        return retrofitBuilder
            .baseUrl("https://dummyurl.com") //no-op as all URLs for webdav are absolute
            .client(okHttpClient)
            .build()
    }

    private fun provideWebDavApi(): WebDavApi {
        val retrofit = provideWebDavRetrofit()
        return retrofit.create(WebDavApi::class.java)
    }

}