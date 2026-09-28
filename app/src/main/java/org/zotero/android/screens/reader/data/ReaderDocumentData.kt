package org.zotero.android.screens.reader.data

import com.google.gson.JsonArray
import java.io.File

data class ReaderDocumentData(
    val type: String,
    val file: File,
    val annotationsJson: JsonArray,
    val page: ReaderPage?,
    val selectedAnnotationKey: String?,
    val savedPdfViewState: ReaderPdfViewState? = null,
    // Fit the PDF content to the screen on open, keeping this margin
    val contentFitMargin: Double? = null,
)