package org.zotero.android.websocket.responses

data class CheckLoginSessionResponse(val status: Status) {

    sealed interface Status {
        object pending: Status
        data class completed(val apiKey: String, val userId: Long, val username: String): Status
        object cancelled: Status
    }
}