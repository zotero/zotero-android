package org.zotero.android.screens.reader.colorpicker.data

import org.zotero.android.screens.reader.data.ReaderAnnotationTool

data class ReaderColorPickerResult(
    val colorHex: String? = null,
    val annotationTool: ReaderAnnotationTool,
    val size: Float? = null,
)
