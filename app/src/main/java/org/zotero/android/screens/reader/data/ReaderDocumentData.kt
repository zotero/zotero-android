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
    // Fit to Content margin, null when it's off
    val contentFitMargin: Double? = null,
    // False when a zoom the user chose is restored instead of fitting
    val fitToContentOnOpen: Boolean = true,
)