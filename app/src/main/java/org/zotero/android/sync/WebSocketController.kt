package org.zotero.android.sync

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.zotero.android.api.ForWebSocket
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.ReadVersionDbRequest
import org.zotero.android.data.mappers.ChangeWsResponseMapper
import org.zotero.android.data.mappers.WsResponseMapper
import org.zotero.android.websocket.ChangeWsResponse
import org.zotero.android.websocket.Command
import org.zotero.android.websocket.SubscribeWsMessage
import org.zotero.android.websocket.UnsubscribeWsMessage
import org.zotero.android.websocket.WsResponse
import timber.log.Timber
import java.lang.Integer.min
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangeWsResponseKindEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<ChangeWsResponse.Kind>(applicationScope)

private data class Response(
    val timer: BackgroundTimer,
    val completion: () -> Unit
) {

    companion object {
        fun create(timeoutMs: Long, completion: (WebSocketController.Error?) -> Unit): Response {
            val timer = BackgroundTimer(timeIntervalMs = timeoutMs, eventHandler = {
                completion(WebSocketController.Error.timedOut)
            })
            timer.resume()

            return Response(timer = timer, completion =  {
                timer.suspend()
                completion(null)
            })
        }
    }


}

@Singleton
class WebSocketController @Inject constructor(
    val dbWrapper: DbWrapper,
    val observable: ChangeWsResponseKindEventStream,
    val dispatcher: CoroutineDispatcher,
    @ForWebSocket
    private val okHttpClient: OkHttpClient,
    private val wsResponseMapper: WsResponseMapper,
    private val changeWsResponseMapper: ChangeWsResponseMapper
) {
    enum class ConnectionState {
        disconnected, connecting, subscribing, connected
    }

    sealed class Error : Throwable() {
        object cantCreateMessage: Error()
        object timedOut: Error()
        object notConnected: Error()
    }
    private val completionTimeout = 1500L // miliseconds
    private val messageTimeout: Int = 30
    private val disconnectionTimeout: Int = 5
    private val retryIntervals: List<Int> = listOf(
        2, 5, 10, 15, 30,
        60, 60, 60, 60,
        120, 120, 120, 120,
        300, 300,
        600,
        1200,
        1800, 1800,
        3600, 3600, 3600,
        14400, 14400, 14400,
        86400
    )

    private var connectionState: ConnectionState = ConnectionState.disconnected

    private var apiKey: String? = null
    private var responseListeners: MutableMap<WsResponse.Event, Response> = mutableMapOf()
    private var connectionRetryCount: Int = 0
    private var connectionTimer: BackgroundTimer? = null
    private var completionAction: (() -> Unit)? = null
    private var completionTimer: BackgroundTimer? = null
    private val url: String = "wss://stream.zotero.org"

    private var coroutineScope = CoroutineScope(dispatcher)

    private var webSocket: WebSocket? = null

    fun connect(apiKey: String, completed: (() -> Unit)? = null) {
        coroutineScope.launch {
            _connect(apiKey = apiKey, completed = completed)
        }
    }

    private fun _connect(apiKey: String, completed: (() -> Unit)?) {
        when(this.connectionState) {
            ConnectionState.subscribing, ConnectionState.connected -> {
                Timber.w("WebSocketController: tried to connect while ${this.connectionState}")
                completed?.let { it() }
                return
            }
            ConnectionState.connecting, ConnectionState.disconnected -> {
                //no-op
            }
        }

        this.apiKey = apiKey

        Timber.i("WebSocketController: connect")
        this.connectionTimer?.suspend()
        this.connectionTimer = null
        this.connectionState = ConnectionState.connecting


        createResponse(WsResponse.Event.connected) { error ->
            processConnectionResponse(error, apiKey = apiKey)
        }
        if (completed != null) {
            this.completionAction = completed

            val completionTimer = BackgroundTimer(timeIntervalMs = this.completionTimeout) {
                this.completionAction?.let { it() }
                this.completionAction = null
                this.completionTimer = null
            }
            completionTimer.resume()
            this.completionTimer = completionTimer
        }

        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(this.url).get().build(),
            object : WebSocketListener() {
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.i("onClosed, code:$code, reason:$reason")
                    reconnect()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.i("onClosing, code:$code, reason:$reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Timber.e(t, "onFailure, response:$response")
                    reconnect()
                }

                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    Timber.i("onOpen")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.i("onMessage, text:$text")
                    handle(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                    handle(bytes.utf8())
                }
            }
        )
    }

    private fun createResponse(event: WsResponse.Event, completion: (Error?) -> Unit) {
        val response =
            Response.create(timeoutMs = this.messageTimeout * 1000L, completion = { error ->
                this.responseListeners.remove(event)
                completion(error)
            })
        this.responseListeners[event] = response
    }

    private fun processConnectionResponse(error: Error?, apiKey: String) {
        if (error != null){
            Timber.e(error, "WebSocketController: connection error)")
            retryConnection(apiKey = apiKey)
            return
        }

        when (this.connectionState) {
            ConnectionState.connecting -> {
                this.connectionState = ConnectionState.subscribing
                Timber.i("WebSocketController: subscribe")
                subscribe(apiKey = apiKey) { error  ->
                    processConnectionResponse(error, apiKey = apiKey)
                }
            }
            ConnectionState.subscribing -> {
                Timber.i("WebSocketController: connected & subscribed")

                this.connectionState = ConnectionState.connected
                this.connectionRetryCount = 0
                this.connectionTimer?.suspend()
                this.connectionTimer = null
                this.completionTimer?.suspend()
                this.completionTimer = null

                this.completionAction?.let { it() }
                this.completionAction = null
            }
            ConnectionState.connected, ConnectionState.disconnected -> {
                Timber.w("WebSocketController: connection response processed while already ${this.connectionState}")
                this.completionAction?.let { it() }
                this.completionAction = null
            }
        }
        return
    }

    private fun retryConnection(apiKey: String) {
        when (this.connectionState) {
            ConnectionState.connected, ConnectionState.disconnected -> {
                Timber.w("WebSocketController: tried to retry connection while already ${this.connectionState}")
                return
            }
            else -> {
                //no-op
            }
        }

        val interval = retryIntervals[min(this.connectionRetryCount, (retryIntervals.size - 1))]
        this.connectionRetryCount += 1
        Timber.i("WebSocketController: schedule retry attempt ${this.connectionRetryCount} interval ${interval}")

        val timer = BackgroundTimer(timeIntervalMs = interval + 1000L) {
            when (this.connectionState){
                ConnectionState.connecting, ConnectionState.disconnected -> {
                    _connect(apiKey = apiKey, completed = null)
                }
                ConnectionState.subscribing -> {
                    subscribe(apiKey = apiKey, completion = {  error ->
                        processConnectionResponse(error, apiKey =apiKey)
                    })
                }
                ConnectionState.connected -> {
                    //no-op
                }
            }

            this.connectionTimer = null
        }
        timer.resume()
        this.connectionTimer = timer
    }

    private fun reconnect() {
        if (this.connectionState != ConnectionState.connected) {
            return
        }
        this.connectionState = ConnectionState.disconnected

        val apiKey = this.apiKey
        if (apiKey == null) {
            Timber.e("WebSocketController: attempting reconnect, but apiKey is missing")
            return
        }

        Timber.i("WebSocketController: schedule reconnect")

        val timer = BackgroundTimer(timeIntervalMs = disconnectionTimeout + 1000L) {
            _connect(apiKey = apiKey, completed = null)
            this.connectionTimer = null
        }
        timer.resume()
        this.connectionTimer = timer
    }

    fun disconnect(apiKey: String?) {
        coroutineScope.launch {
            if (this@WebSocketController.connectionState == ConnectionState.disconnected) {
                return@launch
            }
            if (apiKey != null && this@WebSocketController.connectionState == ConnectionState.connected) {
                unsubscribe(apiKey)
            } else {
                disconnect()
            }
        }
    }

    private fun subscribe(apiKey: String, completion: (Error?) -> Unit) {
        send(message = SubscribeWsMessage(apiKey = apiKey), responseEvent = WsResponse.Event.subscriptionCreated, completion = completion)
    }

    private fun disconnect() {
        this.connectionState = ConnectionState.disconnected
        this.connectionRetryCount = 0
        this.connectionTimer?.suspend()
        this.connectionTimer = null
        this.completionTimer?.suspend()
        this.completionTimer = null
        for ((_, response) in this.responseListeners) {
            response.timer.suspend()
        }
        this.responseListeners = mutableMapOf()
        this.webSocket?.cancel()
        this.webSocket = null
        this.apiKey = null
    }

    private fun unsubscribe(apiKey: String) {
        send(message = UnsubscribeWsMessage(apiKey = apiKey), responseEvent = WsResponse.Event.subscriptionDeleted){
            disconnect()
        }
    }

    private fun handle(textToParse: String) {
        try {
            val event = wsResponseMapper.fromString(textToParse).event
            val response = this.responseListeners[event]
            if (response != null) {
                response.completion()
                return
            }

            Timber.i("WebSocketController: handle event - ${event}")

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
                WsResponse.Event.connected, WsResponse.Event.subscriptionCreated, WsResponse.Event.subscriptionDeleted -> {
                    //no-op
                }
            }

        } catch (error: Throwable) {
            Timber.e(
                error,
                "WebSocketController: received unknown message. Original message: ${textToParse}"
            )
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
                    val localVersion = dbWrapper.realmDbStorage.perform(
                        ReadVersionDbRequest(libraryId = responseType.libraryIdentifier),
                        invalidateRealm = true
                    )
                    if (localVersion >= responseType.version) {
                        return
                    }
                    this.observable.emitAsync(responseType)
                } catch (error: Exception) {
                    Timber.e(error, "WebSocketController: can't read version for received message")
                }
            }

        }
    }

    private fun send(message: Command, responseEvent: WsResponse.Event, completion: (Error?) -> Unit) {
        val webSocket = this.webSocket
        if (this.connectionState == ConnectionState.disconnected || webSocket == null) {
            completion(Error.notConnected)
            return
        }

        try {
            val string = Gson().toJson(message)
            Timber.i("WebSocketController: send message - ${string}")
            createResponse(responseEvent, completion = completion)
            webSocket.send(string)
        } catch (error: Exception) {
            Timber.e(error, "WebSocketController: message error ${message}")
            completion(Error.cantCreateMessage)
        }
    }
}