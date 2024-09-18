package org.zotero.android.pdf.data

import com.pspdfkit.ui.special_mode.controller.AnnotationTool

data class PdfReaderTool(
    val type: AnnotationTool,
    val title: Int,
    val image: Int,
    val isHidden: Boolean,
)