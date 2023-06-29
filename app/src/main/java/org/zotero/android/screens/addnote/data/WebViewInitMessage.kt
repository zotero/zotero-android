package org.zotero.android.screens.addnote.data

data class WebViewInitMessage(val instanceId: Int, val message: WebViewInitPayload) {
    data class WebViewInitPayload(val action: String, val value: String, val readOnly: Boolean)
}