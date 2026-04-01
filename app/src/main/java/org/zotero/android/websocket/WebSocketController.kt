package org.zotero.android.websocket

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.zotero.android.api.annotations.ForWebSocketApi
import org.zotero.android.api.mappers.WsResponseMapper
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.RealmDbStorage
import org.zotero.android.sync.BackgroundTimer
import timber.log.Timber
import java.lang.Integer.min
import javax.inject.Inject

class WebSocketController @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    @ForWebSocketApi
    private val okHttpClient: OkHttpClient,
    private val wsResponseMapper: WsResponseMapper,
    private val gson: Gson,
    private val applicationScope: ApplicationScope,
) {

    val messageObservable: EventStream<String> = EventStream(applicationScope)
    val connectionStateObservable = StateEventStream<ConnectionState>(
        applicationScope = applicationScope,
        initValue = ConnectionState.disconnected
    )

    private data class Response(
        val timer: BackgroundTimer,
        val completion: () -> Unit
    ) {

        companion object {
            fun create(
                timeoutMs: Long,
                completion: (Error?) -> Unit
            ): Response {
                val timer = BackgroundTimer(timeIntervalMs = timeoutMs, eventHandler = {
                    completion(Error.timedOut)
                })
                timer.resume()

                return Response(timer = timer, completion = {
                    timer.suspend()
                    completion(null)
                })
            }
        }

    }

    enum class ConnectionState {
        disconnected, connecting, connected
    }

    sealed class Error : Throwable() {
        object cantCreateMessage : Error()
        object timedOut : Error()
        object notConnected : Error()
    }

    companion object {
        //in seconds
        val retryIntervals: List<Int> = listOf(
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

        val completionTimeout = 1500L // miliseconds
        val messageTimeout: Int = 30 //seconds
        val disconnectionTimeout: Int = 5 //seconds
    }

    private val url: String = "wss://stream.zotero.org"

    private var shouldStayConnected: Boolean = false
    private var redactedValues: Set<String> = emptySet()

    private var responseListeners: MutableMap<WsResponse.Event, Response> = mutableMapOf()
    private var connectionRetryCount: Int = 0
    private var connectionTimer: BackgroundTimer? = null
    private var completionAction: (() -> Unit)? = null
    private var completionTimer: BackgroundTimer? = null

    private var coroutineScope = CoroutineScope(dispatcher)

    private var webSocket: WebSocket? = null

    fun connect(completed: (() -> Unit)? = null) {
        coroutineScope.launch {
            connectInternal(completed = completed)
        }
    }

    private fun connectInternal(completed: (() -> Unit)?) {
        when (this.connectionStateObservable.currentValue()!!) {
            ConnectionState.connected -> {
                Timber.w("WebSocketController: tried to connect while ${this.connectionStateObservable.currentValue()}")
                completed?.let { it() }
                return
            }

            ConnectionState.connecting, ConnectionState.disconnected -> {
                //no-op
            }
        }

        shouldStayConnected = true

        Timber.i("WebSocketController: connect")
        this.connectionTimer?.suspend()
        this.connectionTimer = null
        this.connectionStateObservable.emitAsync(ConnectionState.connecting)

        createResponse(WsResponse.Event.connected) { error ->
            processConnectionResponse(error)
        }

        if (completed != null) {
            this.completionAction = completed

            val completionTimer = BackgroundTimer(timeIntervalMs = completionTimeout) {
                this.completionAction?.let { it() }
                this.completionAction = null
                this.completionTimer = null
            }

            completionTimer.resume()
            this.completionTimer = completionTimer
        }

        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(this.url).get().build(),
            handle()
        )
    }

    private fun processConnectionResponse(error: Error?) {
        if (error != null) {
            if (error !is Error.timedOut) {
                Timber.e(error, "WebSocketController: connection error)")
            }
            retryConnection()
            return
        }

        when (this.connectionStateObservable.currentValue()!!) {
            ConnectionState.connecting -> {
                Timber.i("WebSocketController: connected")

                this.connectionStateObservable.emitAsync(ConnectionState.connected)
                this.connectionRetryCount = 0
                this.connectionTimer?.suspend()
                this.connectionTimer = null

                this.completionTimer?.suspend()
                this.completionTimer = null
                this.completionAction?.let { it() }
                this.completionAction = null
            }

            ConnectionState.connected, ConnectionState.disconnected -> {
                Timber.w("WebSocketController: connection response processed while already ${this.connectionStateObservable.currentValue()}")
                this.completionAction?.let { it() }
                this.completionAction = null
            }
        }
    }

    private fun retryConnection() {
        when (this.connectionStateObservable.currentValue()!!) {
            ConnectionState.connected, ConnectionState.disconnected -> {
                Timber.w("WebSocketController: tried to retry connection while already ${this.connectionStateObservable.currentValue()}")
                return
            }

            ConnectionState.connecting -> {
                //no-op
            }
        }

        val interval = retryIntervals[min(this.connectionRetryCount, (retryIntervals.size - 1))]
        this.connectionRetryCount += 1
        Timber.i("WebSocketController: schedule retry attempt ${this.connectionRetryCount} interval $interval")

        val timer = BackgroundTimer(timeIntervalMs = interval * 1000L) {
            if (!shouldStayConnected) {
                connectionTimer = null
                return@BackgroundTimer
            }
            when (this.connectionStateObservable.currentValue()!!) {
                ConnectionState.disconnected, ConnectionState.connecting -> {
                    connectInternal(completed = null)
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
        if (this.connectionStateObservable.currentValue() != ConnectionState.connected) {
            return
        }
        this.connectionStateObservable.emitAsync(ConnectionState.disconnected)

        if (!shouldStayConnected) {
            Timber.w("WebSocketController: websocket disconnected without reconnect intent")
            return
        }

        Timber.i("WebSocketController: schedule reconnect")

        val timer = BackgroundTimer(timeIntervalMs = disconnectionTimeout * 1000L) {
            connectInternal(completed = null)
            this.connectionTimer = null
        }
        timer.resume()
        this.connectionTimer = timer
    }

    fun disconnect() {
        coroutineScope.launch {
            shouldStayConnected = false
            disconnectInternal()
        }
    }

    private fun disconnectInternal() {
        if (this.connectionStateObservable.currentValue() == ConnectionState.disconnected) {
            return
        }
        this.connectionStateObservable.emitAsync(ConnectionState.disconnected)
        this.connectionRetryCount = 0
        this.connectionTimer?.suspend()
        this.connectionTimer = null

        completionTimer?.suspend()
        completionTimer = null

        for ((_, response) in this.responseListeners) {
            response.timer.suspend()
        }
        this.responseListeners = mutableMapOf()
        this.webSocket?.cancel()
        this.webSocket = null
    }

    fun send(
        message: Any,
        responseEvent: WsResponse.Event,
        completion: (Error?) -> Unit
    ) {
        sendInternal(message, responseEvent, completion)
    }

    private fun sendInternal(
        message: Any,
        responseEvent: WsResponse.Event,
        completion: (Error?) -> Unit
    ) {
        val webSocket = this.webSocket
        if (this.connectionStateObservable.currentValue() == ConnectionState.disconnected || webSocket == null) {
            completion(Error.notConnected)
            return
        }

        try {
            val string = gson.toJson(message)
            Timber.i("WebSocketController: send message - ${redact(string)}")
            createResponse(responseEvent, completion = completion)
            webSocket.send(string)
        } catch (error: Exception) {
            Timber.e(error, "WebSocketController: message error ${redact(message.toString())}")
            completion(Error.cantCreateMessage)
        }
    }

    fun setRedactedValues(values: Set<String>) {
        this.redactedValues = values
    }

    fun createTimer(timeIntervalMs: Long, eventHandler: () -> Unit): BackgroundTimer {
        val timer = BackgroundTimer(timeIntervalMs = timeIntervalMs, eventHandler)
        return timer
    }

    inline fun <reified T : Any> performDbRequest(
        request: DbResponseRequest<T>,
        dbStorage: RealmDbStorage,
        invalidateRealm: Boolean
    ): T {
        return dbStorage.perform(request = request, invalidateRealm = invalidateRealm)
    }

    private fun handle(): WebSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Timber.i("WebSocketController: onClosed, code:$code, reason:$reason")
            reconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Timber.i("WebSocketController: onClosing, code:$code, reason:$reason")
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: okhttp3.Response?
        ) {
            Timber.i(t, "WebSocketController: onFailure, response:$response")
            reconnect()
        }

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            Timber.i("WebSocketController: onOpen")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Timber.i("WebSocketController: onMessage, text:${redact(text)}")
            handle(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            handle(bytes.utf8())
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

            Timber.i("WebSocketController: handle event - ${redact(event.toString())}")
            messageObservable.emitAsync(textToParse)
        } catch (error: Throwable) {
            Timber.e(
                error,
                "WebSocketController: received unknown message. Original message: ${
                    redact(
                        textToParse
                    )
                }"
            )
        }
    }

    private fun createResponse(event: WsResponse.Event, completion: (Error?) -> Unit, ) {
        val response =
            Response.create(timeoutMs = messageTimeout * 1000L, completion = { error ->
                this.responseListeners.remove(event)
                completion(error)
            })
        this.responseListeners[event] = response
    }

    private fun redact(logMessage: String): String {
        var result = logMessage
        for (rV in redactedValues) {
            result = result.replace(rV, "<redacted>")
        }
        return result
    }

}