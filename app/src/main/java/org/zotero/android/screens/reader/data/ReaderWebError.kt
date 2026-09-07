package org.zotero.android.screens.reader.data

sealed class ReaderWebError: Exception() {
    object failedToInitializeWebView: ReaderWebError()
}