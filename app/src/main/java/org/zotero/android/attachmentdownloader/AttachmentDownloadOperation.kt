package org.zotero.android.attachmentdownloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import okhttp3.ResponseBody
import org.zotero.android.BuildConfig
import org.zotero.android.androidx.file.copyWithExt
import org.zotero.android.api.NoRedirectApi
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.helpers.Unzipper
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.WebDavSessionStorage
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.SocketException

class AttachmentDownloadOperation(
    private val file: File,
    private val download: AttachmentDownloader.Download,
    private val userId: Long,
    private val syncApi: SyncApi,
    private val noRedirectApi: NoRedirectApi,
    private val unzipper: Unzipper,
    private val webDavController: WebDavController,
    private val sessionStorage: WebDavSessionStorage,
) {
    private enum class State {
        downloading, unzipping, done
    }

    sealed class Error : Exception() {
        object cancelled : Error()
    }

    private var state: State? = null

    var onDownloadProgressUpdated: OnDownloadProgressUpdated? = null
    var finishedDownload: ((customResult: CustomResult<Unit>) -> Unit)? = null
    var progressInHundreds: Int = 0

    private var coroutineScope: CoroutineScope? = null

    suspend fun start(coroutineScope: CoroutineScope) {
        if (this.state == null && isOperationNotActive()) {
            this.coroutineScope = coroutineScope
            startDownload()
        }
    }

    private fun isOperationNotActive() =
        this.coroutineScope == null || this.coroutineScope?.isActive == false

    private suspend fun startDownload() {
        Timber.i("AttachmentDownloadOperation: start downloading ${this.download.key}")
        this.state = State.downloading

        val shouldUseWebDav =
            this.download.libraryId is LibraryIdentifier.custom && this.sessionStorage.isEnabled
        val networkResult = if (shouldUseWebDav) {
            downloadRequestWebDav(
                key = this.download.key,
            )
        } else {
            downloadRequestZotero(
                key = this.download.key,
                libraryId = this.download.libraryId,
                userId = this.userId
            )
        }


        if (networkResult is CustomResult.GeneralError) {
            processError(networkResult)
            return
        }

        try {
            networkResult as CustomResult.GeneralSuccess
            val isCompressed = networkResult.value!!.second
            val responseBody = networkResult.value!!.first
            val byteStream = responseBody.byteStream()

            val input = BufferedInputStream(byteStream)
            val output = FileOutputStream(file)

            val contentLength = responseBody.contentLength()
            var numOfBytesRead: Int
            var totalNumberOfBytesRead: Long = 0
            val byteArray = ByteArray(1024)

            while (input.read(byteArray).also { numOfBytesRead = it } != -1) {
                totalNumberOfBytesRead += numOfBytesRead
                output.write(byteArray, 0, numOfBytesRead)

                if (isOperationNotActive()) {
                    return
                }
                progressInHundreds  =
                    ((totalNumberOfBytesRead / contentLength.toDouble()) * 100).toInt()
                onDownloadProgressUpdated?.onProgressUpdated(progressInHundreds)
            }
            output.flush()
            output.close()
            input.close()

            if (isOperationNotActive()) {
                return
            }
            this.coroutineScope = null
            state = State.done

            if (isCompressed) {
                state = State.unzipping
                unzip()
                return
            }

            // Finish download
            finish(CustomResult.GeneralSuccess(null))
        } catch (e: Exception) {
            processError(CustomResult.GeneralError.CodeError(e))
        }

    }

    private fun unzip() {
        val result = _unzip(file = this.file)
        this.state = State.done
        finish(result)
    }

    private fun _unzip(file: File): CustomResult<Unit> {
        val zipFile = file.copyWithExt("zip")
        try {
            if (zipFile.exists()) {
                zipFile.delete()
            }
            file.renameTo(zipFile)
            val files = zipFile.parentFile!!.listFiles()
            for (file in files) {
                if (file.name == zipFile.name && file.extension == zipFile.extension) {
                    continue
                }
                file.delete()
            }
            unzipper.unzip(zipFile.absolutePath, zipFile.parent!!)
            zipFile.delete()
            val unzippedFiles = file.parentFile!!.listFiles()
            if (unzippedFiles.size == 1) {
                val unzipped = unzippedFiles.firstOrNull()
                if (unzipped != null && (unzipped.name != file.name || unzipped.extension != file.extension)) {
                    unzipped.renameTo(file)
                }
            }
            if (file.exists()) {
                return CustomResult.GeneralSuccess(Unit)
            }
            return CustomResult.GeneralError.CodeError(AttachmentDownloader.Error.zipDidntContainRequestedFile)
        } catch (error: Exception) {
            Timber.e(error, "AttachmentDownloadOperation: unzip error")
            return CustomResult.GeneralError.CodeError(AttachmentDownloader.Error.cantUnzipSnapshot)
        }
    }

    private suspend fun downloadRequestZotero(key: String, libraryId: LibraryIdentifier, userId: Long): CustomResult<Pair<ResponseBody, Boolean>> {
        var isCompressed: Boolean = sessionStorage.isEnabled && !download.libraryId.isGroupLibrary
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = userId) + "/items/$key/file"

        val headersResponse = noRedirectApi.getRequestHeadersApi(url)
        if (!isCompressed) {
            isCompressed = headersResponse.headers()["Zotero-File-Compressed"] == "Yes"
        }

        val networkResult = safeApiCall {
            syncApi.downloadFile(url)
        }
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess
        return CustomResult.GeneralSuccess(Pair(networkResult.value!!, isCompressed))
    }

    private suspend fun downloadRequestWebDav(key: String): CustomResult<Pair<ResponseBody, Boolean>> {
        val networkResult = webDavController.download(key = key)
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as CustomResult.GeneralSuccess
        return CustomResult.GeneralSuccess(Pair(networkResult.value!!, true))
    }

    private fun finish(result: CustomResult<Unit>) {
        Timber.i("AttachmentDownloadOperation: finished downloading ${this.download.key}")
        finishedDownload?.let { it(result)}
    }

    fun cancel() {
        Timber.i("AttachmentDownloadOperation: cancelled ${this.download.key}")
        val localState = state
        if (localState == null) {
            finishedDownload?.let { it(
                CustomResult.GeneralError.CodeError(
                    Error.cancelled
                )
            ) }
            return
        }
        state = null

        when (localState) {
            State.downloading -> {
                this.coroutineScope?.cancel()
                this.coroutineScope = null
            }
            State.done -> {
            }
            State.unzipping -> {
                this.coroutineScope?.cancel()
                this.coroutineScope = null
            }
        }

        finishedDownload?.let { it(
            CustomResult.GeneralError.CodeError(
                Error.cancelled
            )
        )}
    }

    private fun processError(error: CustomResult.GeneralError) {
        when (error) {
            is CustomResult.GeneralError.CodeError -> {
                //Do not error-log SocketException as it's just a network interruption
                if (error.throwable !is SocketException) {
                    Timber.e(error.throwable)
                }
            }
            is CustomResult.GeneralError.NetworkError -> {
                //Do not error-log "Attachment Not Found" network errors, as it's not an error
                if (!error.isNotFound()) {
                    Timber.e(error.stringResponse)
                }
            }
        }
        if (isOperationNotActive()) {
            return
        }
        if (file.exists()) {
            file.deleteRecursively()
        }

        this.coroutineScope = null
        state = State.done
        finish(error)
    }
}
