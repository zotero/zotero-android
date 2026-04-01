data class SubscribeWsMessage(
    val action: String,
    val subscriptions: List<Subscription>
) {
    data class Subscription(
        val apiKey: String? = null,
        val topics: List<String>? = null,
    )

    companion object {
        fun init(subscription: Subscription): SubscribeWsMessage {
            return SubscribeWsMessage(
                action = "createSubscriptions",
                subscriptions = listOf(subscription)
            )
        }

        fun initApiKey(apiKey: String): SubscribeWsMessage {
            return init(subscription = Subscription(apiKey = apiKey))
        }

        fun initTopic(topic: String): SubscribeWsMessage {
            return init(subscription = Subscription(topics = (listOf(topic))))
        }
    }
}
