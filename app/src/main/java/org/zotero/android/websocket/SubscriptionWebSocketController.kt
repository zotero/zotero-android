package org.zotero.android.websocket

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.RealmDbStorage
import org.zotero.android.sync.BackgroundTimer
import org.zotero.android.websocket.WebSocketController.ConnectionState
import timber.log.Timber

abstract class SubscriptionWebSocketController constructor(
    open val transport: WebSocketController,
    private val dispatcher: CoroutineDispatcher,
) {

    private enum class SubscriptionState {
        disconnected, subscribing, subscribed
    }

    private var subscriptionValue: String? = null
    private var subscriptionState: SubscriptionState = SubscriptionState.disconnected
    private var retryCount: Int = 0
    private var retryTimer: BackgroundTimer? = null
    private var completionAction: (() -> Unit)? = null

    private var coroutineScope = CoroutineScope(dispatcher)

    fun init() {
        transport.connectionStateObservable
            .flow()
            .onEach {
                connectionStateChanged(it!!)
            }
            .launchIn(coroutineScope)

        transport.messageObservable
            .flow()
            .onEach {
                handleTransportData(it)
            }
            .launchIn(coroutineScope)
    }

    fun connect(subscriptionValue: String, completed: (() -> Unit)? = null) {
        this.subscriptionValue = subscriptionValue
        this.subscriptionState = SubscriptionState.disconnected
        this.completionAction = completed
        transport.setRedactedValues(setOf(subscriptionValue))

        if (transport.connectionStateObservable.currentValue() == ConnectionState.connected) {
            subscribeIfNeeded()
            return
        }

        transport.connect()
    }

    fun disconnect(subscriptionValue: String? = null) {
        completionAction = null
        resetRetryState()

        val subscriptionValue = subscriptionValue ?: this.subscriptionValue
        if (subscriptionValue == null || transport.connectionStateObservable.currentValue() != ConnectionState.connected) {
            clearSubscription()
            transport.disconnect()
            return
        }

        unsubscribe(subscriptionValue) {
            clearSubscription()
            transport.disconnect()
        }
    }

    abstract fun handleTransportData(textToParse: String)

    abstract fun logCategory(): String

    abstract fun subscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    )

    abstract fun unsubscribe(
        subscriptionValue: String,
        completion: (WebSocketController.Error?) -> Unit
    )

    fun didSubscribe() {

    }

    inline fun <reified T : Any> performDbRequest(
        request: DbResponseRequest<T>,
        dbStorage: RealmDbStorage,
        invalidateRealm: Boolean
    ): T {
        return transport.performDbRequest(
            request = request,
            dbStorage = dbStorage,
            invalidateRealm = invalidateRealm
        )
    }


    private fun connectionStateChanged(state: ConnectionState) {
        if (this.subscriptionValue == null) {
            return
        }

        when (state) {
            ConnectionState.connecting -> {
                subscribeIfNeeded()
            }

            ConnectionState.connected, ConnectionState.disconnected -> {
                subscriptionState = SubscriptionState.disconnected
            }
        }
    }

    private fun subscribeIfNeeded() {
        if (this.subscriptionValue == null) {
            return
        }
        if (this.subscriptionState == SubscriptionState.subscribing) {
            return
        }

        subscriptionState = SubscriptionState.subscribing
        Timber.w("${logCategory()}: subscribe")

        subscribe(this.subscriptionValue!!) { error ->
            processSubscriptionResponse(error)
        }
    }

    private fun processSubscriptionResponse(error: WebSocketController.Error?) {
        if (error != null) {
            Timber.e(error, "${logCategory()}: subscription error")
            this.subscriptionState = SubscriptionState.disconnected
            retrySubscriptionIfNeeded()
            return
        }

        Timber.i("${logCategory()}: connected & subscribed")
        this.subscriptionState = SubscriptionState.subscribed
        resetRetryState()
        didSubscribe()
        this.completionAction?.let { it() }
        this.completionAction = null
    }

    private fun retrySubscriptionIfNeeded() {
        if (this.subscriptionValue == null) {
            return
        }

        val interval = WebSocketController.retryIntervals[retryCount.coerceAtMost(
            WebSocketController.retryIntervals.size - 1
        )]
        retryCount += 1
        Timber.i("${logCategory()}: schedule retry attempt $retryCount interval $interval")

        val timer = transport.createTimer(interval * 1000L) {
            this@SubscriptionWebSocketController.retryTimer = null
            if (this.subscriptionValue == null) {
                return@createTimer
            }

            when (transport.connectionStateObservable.currentValue()!!) {
                ConnectionState.connected -> {
                    subscribeIfNeeded()
                }

                ConnectionState.connecting -> {
                    //no-op
                }

                ConnectionState.disconnected -> {
                    transport.connect()
                }
            }
        }
        timer.resume()
        this.retryTimer = timer
    }

    private fun resetRetryState() {
        this.retryCount = 0
        this.retryTimer?.suspend()
        this.retryTimer = null
    }

    private fun clearSubscription() {
        this.subscriptionValue = null
        this.subscriptionState = SubscriptionState.disconnected
        this.transport.setRedactedValues(emptySet())
    }


}