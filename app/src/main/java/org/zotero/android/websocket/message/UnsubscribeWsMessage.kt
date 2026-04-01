data class UnsubscribeWsMessage(
    val action: String,
    val subscriptions: List<Subscription>
) {
    sealed interface Subscription {
        data class apiKey(val key: String) : Subscription
        data class topic(val topic: String) : Subscription
    }

    companion object {
        fun init(subscription: Subscription): UnsubscribeWsMessage {
            return UnsubscribeWsMessage(
                action = "deleteSubscriptions",
                subscriptions = listOf(subscription)
            )
        }

        fun initApiKey(apiKey: String): UnsubscribeWsMessage {
            return init(subscription = Subscription.apiKey(apiKey))
        }

        fun initTopic(topic: String): UnsubscribeWsMessage {
            return init(subscription = Subscription.topic(topic))
        }
    }
}
