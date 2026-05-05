package org.zotero.android.websocket

import SubscribeWsMessage
import UnsubscribeWsMessage
import kotlinx.coroutines.CoroutineDispatcher
import org.zotero.android.api.mappers.WsResponseMapper
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.websocket.mappers.LoginCompleteWsResponseMapper
import org.zotero.android.websocket.responses.LoginWsResponse
import timber.log.Timber
import javax.inject.Inject

class LoginSessionWebSocketController @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val loginCompleteWsResponseMapper: LoginCompleteWsResponseMapper,
    private val wsResponseMapper: WsResponseMapper,
    override val transport: WebSocketController,
    private val applicationScope: ApplicationScope,
) : SubscriptionWebSocketController(
    dispatcher = dispatcher,
    transport = transport,
) {
    val loginObservable: EventStream<LoginWsResponse.Kind> = EventStream(applicationScope)

    companion object {
        fun topic(sessionToken: String): String {
            return "login-session:$sessionToken"
        }
    }

    override fun logCategory(): String {
        return "LoginSessionWebSocketController"
    }

    override fun subscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    ) {
        transport.send(
            message = SubscribeWsMessage.initTopic(topic = topic(subscriptionValue)),
            responseEvent = WsResponse.Event.subscriptionCreated,
            completion = completion
        )
    }

    override fun unsubscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    ) {
        transport.send(
            message = UnsubscribeWsMessage.initTopic(topic = topic(subscriptionValue)),
            responseEvent = WsResponse.Event.subscriptionDeleted,
            completion = completion
        )
    }

    override fun handleTransportData(textToParse: String) {
        try {
            val event = wsResponseMapper.fromString(textToParse).event
            when (event) {
                WsResponse.Event.loginComplete, WsResponse.Event.loginCancelled -> {
                    val response = loginCompleteWsResponseMapper.fromString(textToParse)
                    loginObservable.emitAsync(response.kind)
                }

                WsResponse.Event.topicAdded, WsResponse.Event.topicRemoved, WsResponse.Event.topicUpdated, WsResponse.Event.connected, WsResponse.Event.subscriptionCreated, WsResponse.Event.subscriptionDeleted -> {
                    //no-op
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "LoginSessionWebSocketController: received unknown message")
        }
    }
}