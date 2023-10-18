package org.zotero.android.screens.addnote.data

data class WebViewSendMessage(val instanceId: Int, val message: WebViewSendMessagePayload) {
    data class WebViewSendMessagePayload(val action: String, val value: String, val readOnly: Boolean)
}