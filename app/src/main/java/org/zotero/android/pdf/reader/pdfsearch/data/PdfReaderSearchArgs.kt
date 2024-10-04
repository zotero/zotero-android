package org.zotero.android.pdf.reader.pdfsearch.data

import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.document.PdfDocument

data class PdfReaderSearchArgs(
    val pdfDocument: PdfDocument,
    val configuration: PdfConfiguration
)