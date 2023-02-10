package org.zotero.android.attachmentdownloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.CustomResult.GeneralError.CodeError
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetMimeTypeUseCase
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class RemoteAttachmentDownloadOperation constructor(
    private val noAuthenticationApi: NoAuthenticationApi,
    private val fileStorage: FileStore,
    private val getMimeTypeUseCase: GetMimeTypeUseCase,
    private val url: String,
    private val file: File,
) {

    private enum class State {
        downloading, done
    }

    sealed class Error : Exception() {
        object downloadNotPdf : Error()
        object cancelled : Error()
    }

    private var state: State? = null

    private var coroutineScope: CoroutineScope? = null

    var onDownloadProgressUpdated: OnDownloadProgressUpdated? = null
    var finishedDownload: ((customResult: CustomResult<Unit>) -> Unit)? = null
    var progressInHundreds: Int? = null

    suspend fun start(coroutineScope: CoroutineScope) {
        if (this.state == null && isOperationNotActive()) {
            this.coroutineScope = coroutineScope
            startDownload()
        }
    }

    fun isOperationNotActive() =
        this.coroutineScope == null || this.coroutineScope?.isActive == false

    private suspend fun startDownload() {
        Timber.w("RemoteAttachmentDownloadOperation: start downloading ${this.url}")

        this.state = State.downloading

        val networkResult = safeApiCall {
            noAuthenticationApi.downloadFile(url)
        }

        if (networkResult is CustomResult.GeneralError) {
            processError(networkResult)
            return
        }

        try {
            networkResult as CustomResult.GeneralSuccess.NetworkSuccess
            val responseBody = networkResult.value!!
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
                progressInHundreds = ((totalNumberOfBytesRead / contentLength.toDouble()) * 100).toInt()
                onDownloadProgressUpdated?.onProgressUpdated(progressInHundreds!!)
            }
            output.flush()
            output.close()
            input.close()

            if (isOperationNotActive()) {
                return
            }
            this.coroutineScope = null
            state = State.done

            val fileResponseError = checkFileResponse(file)
            if (fileResponseError != null) {
                file.deleteRecursively()
                finish(CodeError(fileResponseError))
                return
            }

            // Finish download
            finish(CustomResult.GeneralSuccess(null))
        } catch (e: Exception) {
            processError(CodeError(e))
        }

    }

    private fun processError(error: CustomResult.GeneralError) {
        when (error) {
            is CodeError -> Timber.e(error.throwable)
            is CustomResult.GeneralError.NetworkError -> Timber.e(error.stringResponse)
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

    private fun finish(result: CustomResult<Unit>) {
        Timber.w("RemoteAttachmentDownloadOperation: finished downloading $url")
        finishedDownload?.let { it(result)}
    }

    private suspend fun checkFileResponse(file: File): Exception? {
        val mimeType = getMimeTypeUseCase.execute(file.absolutePath)
        if (mimeType == "application/pdf" && !fileStorage.isPdf(file = file)) {
            return Error.downloadNotPdf
        }
        return null
    }


    fun cancel() {
        Timber.i("RemoteAttachmentDownloadOperation: cancelled $url")
        val localState = state
        if (localState == null) {
            finishedDownload?.let { it(CodeError(Error.cancelled)) }
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
        }

        finishedDownload?.let { it(CodeError(Error.cancelled))}
    }

}