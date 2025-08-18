package org.zotero.android.websocket

data class WsResponse(
    val event: Event
) {
    enum class Event(val n: String) {
        connected("connected"),
        subscriptionCreated("subscriptionsCreated"),
        subscriptionDeleted("subscriptionsDeleted"),

        topicAdded("topicAdded"),
        topicRemoved("topicRemoved"),
        topicUpdated("topicUpdated");

        companion object {
            private val map = entries.associateBy(Event::n)

            fun from(n: String) = map[n]
        }
    }

    sealed class Error : Throwable() {
        data class unknownEvent(val text: String) : Error()
    }
}
