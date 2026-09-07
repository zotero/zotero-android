package org.zotero.android.screens.reader.data

import com.google.gson.JsonArray
import java.io.File

data class ReaderDocumentData(
    val type: String,
    val file: File,
    val annotationsJson: JsonArray,
    val page: ReaderPage?,
    val selectedAnnotationKey: String?,
)