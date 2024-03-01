package org.zotero.android.translator.data

sealed class WebViewError : Exception() {

    object webViewMissing: WebViewError()
    object urlMissingTranslators: WebViewError()
}
