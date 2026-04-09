package org.zotero.android.screens.htmlepub.reader.data

import com.google.gson.JsonArray
import java.io.File

data class DocumentData(
    val type: String,
    val file: File,
    val annotationsJson: JsonArray,
    val page: Page?,
    val selectedAnnotationKey: String?,
) {
}

sealed interface Page {
    data class html(val scrollYPercent: Double): Page
    data class epub(val cfi: String): Page
}