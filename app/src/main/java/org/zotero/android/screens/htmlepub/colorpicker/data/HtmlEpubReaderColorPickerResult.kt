package org.zotero.android.screens.htmlepub.colorpicker.data

import org.zotero.android.screens.htmlepub.reader.data.AnnotationTool


data class HtmlEpubReaderColorPickerResult(
    val colorHex: String? = null,
    val annotationTool: AnnotationTool
)
