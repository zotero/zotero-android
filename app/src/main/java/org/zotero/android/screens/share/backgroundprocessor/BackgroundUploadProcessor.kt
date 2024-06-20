package org.zotero.android.screens.share.backgroundprocessor

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.zotero.android.BuildConfig
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.logging.DeviceInfoProvider
import org.zotero.android.backgrounduploader.BackgroundUpload
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.screens.share.service.ShareUploadService
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundUploadProcessor @Inject constructor(
    private val noAuthenticationApi: NoAuthenticationApi,
    private val syncApi: SyncApi,
    private val schemaController: SchemaController,
    private val dbWrapper: DbWrapper,
    private val context: Context,
) {

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)

    private var resultsProcessorCoroutineScope = CoroutineScope(limitedParallelismDispatcher)

    private var uploadsInProgressCount: AtomicInteger = AtomicInteger(0)

    fun startAsync(
        upload: BackgroundUpload,
        filename: String,
        mimeType: String,
        parameters: LinkedHashMap<String, String>,
        headers: Map<String, String>
    ) {
        if (uploadsInProgressCount.get() == 0) {
            ShareUploadService.start(context)
        }
        uploadsInProgressCount.addAndGet(1)
        resultsProcessorCoroutineScope.launch {
            val result = start(
                upload = upload,
                filename = filename,
                mimeType = mimeType,
                parameters = parameters,
                headers = headers
            )

            if (result is CustomResult.GeneralError.CodeError) {
                Timber.e(result.throwable, "Couldn't finish tasks, code error")
            } else if (result is CustomResult.GeneralError.NetworkError) {
                Timber.e("Couldn't finish tasks, network error: %s", result.stringResponse)
            }
            uploadsInProgressCount.decrementAndGet()
            if (uploadsInProgressCount.get() == 0) {
                ShareUploadService.stop(context)
            }

        }
    }

    private suspend fun start(
        upload: BackgroundUpload,
        filename: String,
        mimeType: String,
        parameters: LinkedHashMap<String, String>,
        headers: Map<String, String>
    ): CustomResult<Unit> {

        val uploadAttachmentNetworkResult = safeApiCall {
            val requestBody = createRequestBody(upload.fileUrl, mimeType)
            val part = createPart(filename, requestBody)

            val headersWithExtra = setupHeaders(originalHeaders = headers)

            noAuthenticationApi.uploadAttachment(
                url = upload.remoteUrl,
                headers = headersWithExtra,
                file = part,
                params = parameters
            )
        }

        if (uploadAttachmentNetworkResult !is CustomResult.GeneralSuccess) {
            return uploadAttachmentNetworkResult
        }

        return when (upload.type) {
            is BackgroundUpload.Kind.zotero -> {
                finishZoteroUpload(
                    uploadKey = upload.type.uploadKey,
                    key = upload.key,
                    libraryId = upload.libraryId,
                    userId = upload.userId,
                )
            }

            is BackgroundUpload.Kind.webdav -> {
                //TODO implement webdav
                CustomResult.GeneralSuccess(null)
            }
        }
    }

    private suspend fun finishZoteroUpload(
        uploadKey: String,
        key: String,
        libraryId: LibraryIdentifier,
        userId: Long
    ): CustomResult<Unit> {

        val registerUploadNetworkResult = safeApiCall {
            val headers = mapOf("If-None-Match" to "*")
            val url =
                BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = userId) + "/items/" + key + "/file"
            syncApi.registerUpload(
                url = url,
                headers = headers,
                upload = uploadKey
            )
        }
        if (registerUploadNetworkResult is CustomResult.GeneralError) {
            return registerUploadNetworkResult
        }
        registerUploadNetworkResult as CustomResult.GeneralSuccess.NetworkSuccess
        val lastModifiedVersion = registerUploadNetworkResult.lastModifiedVersion
        markAttachmentAsUploaded(version = lastModifiedVersion, key = key, libraryId = libraryId)
        return CustomResult.GeneralSuccess(Unit)
    }

    private fun setupHeaders(originalHeaders: Map<String, String>): Map<String, String> {
        val headersWithExtra = originalHeaders.toMutableMap()
//        headersWithExtra["Content-Type"] = contentType
        headersWithExtra["Zotero-API-Version"] = 3.toString()
        headersWithExtra["Zotero-Schema-Version"] = schemaController.version.toString()
        headersWithExtra["User-Agent"] = DeviceInfoProvider.userAgentString
        return headersWithExtra

    }

    private fun markAttachmentAsUploaded(
        version: Int?,
        key: String,
        libraryId: LibraryIdentifier,
    ) {
        try {
            dbWrapper.realmDbStorage.perform(
                MarkAttachmentUploadedDbRequest(
                    libraryId = libraryId,
                    key = key,
                    version = version
                )
            )
        } catch (error: Exception) {
            Timber.e("BackgroundUploadProcessor: can't mark attachment as uploaded - $error")
            throw error
        }

    }

    private fun createRequestBody(file: File, mimeType: String): RequestBody {
        val mediaType = mimeType.toMediaTypeOrNull()

        val requestBody = file.asRequestBody(mediaType)
        return requestBody
    }

    private fun createPart(fileName: String, requestBody: RequestBody): MultipartBody.Part {
        return MultipartBody.Part.createFormData("file", fileName, requestBody)
    }

    fun cancelAllUploads() {
        this.resultsProcessorCoroutineScope.cancel()
        this.resultsProcessorCoroutineScope = CoroutineScope(limitedParallelismDispatcher)
    }
}