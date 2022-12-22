package org.zotero.android.websocket

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
data class Command(
    @Keep
    val action: String,
    @Keep
    val subscriptions: List<Map<String, String>>
)

internal fun SubscribeWsMessage(
    apiKey: String
): Command = Command(
    "createSubscriptions", listOf(mapOf("apiKey" to apiKey))
)

internal fun UnsubscribeWsMessage(
    apiKey: String
): Command = Command(
    "deleteSubscriptions", listOf(mapOf("apiKey" to apiKey))
)
