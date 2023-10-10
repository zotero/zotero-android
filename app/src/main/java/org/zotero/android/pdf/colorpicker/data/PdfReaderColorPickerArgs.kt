package org.zotero.android.pdf.colorpicker.data

import com.pspdfkit.ui.special_mode.controller.AnnotationTool

data class PdfReaderColorPickerArgs(
    val tool: AnnotationTool,
    val colorHex: String? = null,
    val size: Float? = null,
)