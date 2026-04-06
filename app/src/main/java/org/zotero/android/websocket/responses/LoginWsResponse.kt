package org.zotero.android.websocket.responses

data class LoginWsResponse(
    val kind: Kind
) {
    sealed interface Kind {
        data class complete(val topicK: String, val userId: Long, val username: String, val apiKey: String): Kind
        data class cancelled(val topicK: String): Kind

        val topic: String get() {
            return when(this) {
                is complete -> {
                    this.topicK
                }

                is cancelled -> {
                    this.topicK
                }
            }
        }
    }
}