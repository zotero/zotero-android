package org.zotero.android.speech.data

data class TextRange(val location: Int, val length: Int) {
    val end: Int get() = location + length
}
