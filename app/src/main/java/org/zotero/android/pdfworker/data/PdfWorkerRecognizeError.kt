package org.zotero.android.pdfworker.data

sealed class PdfWorkerRecognizeError: Exception() {
    data class recognizeFailed(val errorMessage: String): PdfWorkerRecognizeError()
    object failedToInitializePdfWorker: PdfWorkerRecognizeError()
}