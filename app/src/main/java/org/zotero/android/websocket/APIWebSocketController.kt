package org.zotero.android.websocket

import SubscribeWsMessage
import UnsubscribeWsMessage
import kotlinx.coroutines.CoroutineDispatcher
import org.zotero.android.api.mappers.ChangeWsResponseMapper
import org.zotero.android.api.mappers.WsResponseMapper
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadVersionDbRequest
import timber.log.Timber
import javax.inject.Inject

class APIWebSocketController @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val dispatcher: CoroutineDispatcher,
    private val observable: ChangeWsResponseKindEventStream,
    private val wsResponseMapper: WsResponseMapper,
    private val changeWsResponseMapper: ChangeWsResponseMapper,
    override val transport: WebSocketController,
) : SubscriptionWebSocketController(
    dispatcher = dispatcher,
    transport = transport,
) {

    override fun logCategory(): String {
        return "APIWebSocketController"
    }

    override fun subscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    ) {
        transport.send(
            message = SubscribeWsMessage.initApiKey(apiKey = subscriptionValue),
            responseEvent = WsResponse.Event.subscriptionCreated,
            completion = completion
        )
    }

    override fun unsubscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    ) {
        transport.send(
            message = UnsubscribeWsMessage.initApiKey(apiKey = subscriptionValue),
            responseEvent = WsResponse.Event.subscriptionDeleted,
            completion = completion
        )
    }

    override fun handleTransportData(textToParse: String) {
        try {
            val event = wsResponseMapper.fromString(textToParse).event
            when (event) {
                WsResponse.Event.topicAdded, WsResponse.Event.topicRemoved, WsResponse.Event.topicUpdated -> {
                    try {
                        val changeResponse = changeWsResponseMapper.fromString(textToParse)
                        this.publishChangeIfNeeded(response = changeResponse)
                    } catch (e: Exception) {
                        Timber.e(e)
                        return
                    }

                }

                WsResponse.Event.connected, WsResponse.Event.subscriptionCreated, WsResponse.Event.subscriptionDeleted, WsResponse.Event.loginComplete, WsResponse.Event.loginCancelled -> {
                    //no-op
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "APIWebSocketController: received unknown message")
        }
    }

    private fun publishChangeIfNeeded(response: ChangeWsResponse) {
        val responseType = response.type
        when (responseType) {
            is ChangeWsResponse.Kind.translators -> {
                this.observable.emitAsync(responseType)
            }

            is ChangeWsResponse.Kind.library -> {
                if (responseType.version == null) {
                    this.observable.emitAsync(responseType)
                    return
                }

                try {
                    val localVersion = dbWrapperMain.realmDbStorage.perform(
                        ReadVersionDbRequest(libraryId = responseType.libraryIdentifier),
                        invalidateRealm = true
                    )
                    if (localVersion >= responseType.version) {
                        return
                    }
                    this.observable.emitAsync(responseType)
                } catch (error: Exception) {
                    Timber.e(
                        error,
                        "APIWebSocketController: can't read version for received message"
                    )
                }
            }

        }
    }

}