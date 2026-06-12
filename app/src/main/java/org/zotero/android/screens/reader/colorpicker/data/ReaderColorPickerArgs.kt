package org.zotero.android.screens.reader.colorpicker.data

import org.zotero.android.screens.reader.data.ReaderAnnotationTool

data class ReaderColorPickerArgs(
    val tool: ReaderAnnotationTool,
    val colorHex: String? = null,
    val size: Float? = null,
)