package org.zotero.android.pdfworker.data

import com.google.gson.JsonObject

sealed interface PdfWorkerRecognizedData {
    data class itemWithIdentifier(val identifier: String, val item: JsonObject) : PdfWorkerRecognizedData
    data class fallbackItem(val rawData: JsonObject) : PdfWorkerRecognizedData
    object recognizedDataIsEmpty: PdfWorkerRecognizedData
}