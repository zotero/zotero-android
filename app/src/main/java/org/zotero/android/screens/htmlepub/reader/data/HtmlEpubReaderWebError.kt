package org.zotero.android.screens.htmlepub.reader.data

sealed class HtmlEpubReaderWebError: Exception() {
    object failedToInitializeWebView: HtmlEpubReaderWebError()
}