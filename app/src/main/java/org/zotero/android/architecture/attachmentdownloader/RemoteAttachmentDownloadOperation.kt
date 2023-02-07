package org.zotero.android.architecture.attachmentdownloader

import kotlinx.coroutines.Job
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
    private var requestJob: Job? = null
) {

    private enum class State {
        downloading, done
    }

    sealed class Error : Exception() {
        object downloadNotPdf : Error()
        object cancelled : Error()
    }

    private var state: State? = null

    private var progress: Progress? = null

    var progressHandler: ((progress: Progress) -> Void)? = null
    var finishedDownload: ((result: CustomResult<Unit>) -> Void)? = null

    suspend fun start() {
        if (this.state == null && (requestJob == null || requestJob?.isCancelled == false)) {
            startDownload()
        }
    }

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

                if (requestJob?.isCancelled == true) {
                    return
                }
                val currentProgressInHundreds =
                    ((totalNumberOfBytesRead / contentLength.toDouble()) * 100).toInt()
                val downloadProgress = Progress(currentProgressInHundreds)
                progress = downloadProgress
                progressHandler?.let { it(downloadProgress) }
            }
            output.flush()
            output.close()
            input.close()

            if (requestJob?.isCancelled == true) {
                return
            }
            requestJob = null
            state = State.done

            val fileResponseError = checkFileResponse(file)
            if (fileResponseError != null) {
                file.delete()
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
        if (requestJob?.isCancelled == true) {
            return
        }
        if (file.exists()) {
            file.delete()
        }

        requestJob = null
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
                requestJob?.cancel()
                requestJob = null
            }
            State.done -> {
            }
        }

        finishedDownload?.let { it(CodeError(Error.cancelled))}
    }

}