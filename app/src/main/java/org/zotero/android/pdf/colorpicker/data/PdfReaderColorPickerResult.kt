package org.zotero.android.pdf.colorpicker.data

import com.pspdfkit.ui.special_mode.controller.AnnotationTool

data class PdfReaderColorPickerResult(val colorHex: String? = null, val size: Float? = null, val annotationTool: AnnotationTool)
