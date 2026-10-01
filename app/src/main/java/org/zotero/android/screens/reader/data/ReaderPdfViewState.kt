package org.zotero.android.screens.reader.data

// Zoom and scroll position of a PDF as reported by the reader, restored when it's reopened
data class ReaderPdfViewState(
    val pageIndex: Int,
    // Percentage
    val scale: Double,
    // Point at the top-left of the viewport, in PDF page coordinates
    val top: Double?,
    val left: Double?,
)
