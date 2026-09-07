package org.zotero.android.screens.reader.data

sealed interface ReaderPage {
    data class html(val scrollYPercent: Double): ReaderPage
    data class epub(val cfi: String): ReaderPage
    data class pdf(val pageIndex: Int): ReaderPage
}