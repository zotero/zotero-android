package org.zotero.android.websocket.responses

data class CreateLoginSessionResponse(
    val sessionToken: String,
    val loginUrl: String,
)
