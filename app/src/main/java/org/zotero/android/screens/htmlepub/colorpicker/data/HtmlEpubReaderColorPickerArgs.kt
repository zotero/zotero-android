package org.zotero.android.screens.htmlepub.colorpicker.data

import org.zotero.android.screens.htmlepub.reader.data.AnnotationTool

data class HtmlEpubReaderColorPickerArgs(
    val tool: AnnotationTool,
    val colorHex: String? = null,
)