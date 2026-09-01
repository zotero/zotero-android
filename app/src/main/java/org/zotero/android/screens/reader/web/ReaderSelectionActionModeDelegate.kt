package org.zotero.android.screens.reader.web

interface ReaderSelectionActionModeDelegate {
    fun hasValidSelection(): Boolean
    fun onHighlight()
    fun onUnderline()
    fun onCopy(): Boolean
}