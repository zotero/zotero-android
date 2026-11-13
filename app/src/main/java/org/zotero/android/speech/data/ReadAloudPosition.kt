package org.zotero.android.speech.data

sealed class ReadAloudPosition {
    data class Pdf(val rects: List<SDTRect>) : ReadAloudPosition()
    data class HtmlEpub(val sdtStart: List<Int>, val sdtEnd: List<Int>) : ReadAloudPosition()
}