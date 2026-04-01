package org.zotero.android.websocket.responses

data class LoginCompleteWsResponse(
    val topic: String,
    val userId: Long,
    val username: String,
    val apiKey: String,
)