package org.zotero.android.architecture

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.DbWrapper
import timber.log.Timber

abstract class BaseViewModel2<STATE : ViewState, EFFECT : ViewEffect>(
    private val initialState: STATE,
    private val loggingEnabled: Boolean = false,
) : ViewModel() {

    private var isInitialized = false

    private val initialThreadName: String =
        Thread
            .currentThread()
            .name
    private val internalState: MutableLiveData<STATE> =
        MutableLiveData(initialState)
    val viewStates: LiveData<STATE> = internalState

    val viewState: STATE
        get() = viewStates.value ?: initialState

    private val internalEffects: MutableLiveData<Consumable<EFFECT>> =
        MutableLiveData()
    val viewEffects: LiveData<Consumable<EFFECT>> = internalEffects

    /**
     * Use to relax thread check in unit tests.
     * When running coroutines in unit tests on local JVM (in blocked mode) there is no guarantee
     * that code will be invoked on the exact same thread where the class was created.
     * When running coroutines on Android, the scheduler works as expected.
     */
    @VisibleForTesting
    var checkMainThread = true

    private fun mainThreadCheck() {
        if (!checkMainThread) return

        check(
            Thread.currentThread().name == initialThreadName
        ) {
            "Don't update state off the main thread!" +
                " Current thread = ${Thread.currentThread().name}"
        }
    }

    /**
     * [updateState] is the only place where a ViewModel can access the
     * [ViewState] and modify it. Once the state is updated, it will be available
     * to observers via the [viewStates] [LiveData], or through the convenience
     * methods [observeViewChanges].
     */
    protected fun updateState(update: STATE.() -> STATE) {
        mainThreadCheck()
        val currentState: STATE = internalState.value ?: initialState
        val updatedState: STATE = currentState.update()
        if (loggingEnabled) {
            Timber.tag(this::class.java.simpleName)
            Timber.d("[State] ${updatedState.sanitizedToString()}")
        }
        internalState.value = updatedState
    }

    /**
     * [triggerEffect] is how a [ViewModel] notifies its observers of one off
     * view effects such as navigation or showing an error message. The
     * [ViewEffect] will be available to observers via the [viewEffects]
     * [LiveData], or through the convenience method [observeViewChanges].
     */
    protected fun triggerEffect(effect: EFFECT) {
        mainThreadCheck()
        if (loggingEnabled) {
            Timber.tag(this::class.java.simpleName)
            Timber.d("[Effect] ${effect.sanitizedToString()}")
        }
        internalEffects.value = Consumable(effect)
    }

    /**
     * [observeViewChanges] is how a screen (whether it be an Activity, Fragment,
     * or other entity) can observe new [ViewState]s or [ViewEffect]s.
     *
     * @see [Screen]
     */
    fun observeViewChanges(screen: Screen<STATE, EFFECT>) {
        viewStates.subscribe(screen.lifecycleOwner, screen::render)
        viewEffects.subscribe(screen.lifecycleOwner, screen::consumeEffect)
    }

    fun observeViewEffects(consumer: ViewEffectConsumer<EFFECT>) {
        viewEffects.subscribe(consumer.lifecycleOwner, consumer::consumeEffect)
    }

    /**
     * Runs the [block] only once regardless of how may times [initOnce] is called.
     * Must be used for init-like functions only to make sure ViewModel is initialized only once.
     */
    protected fun initOnce(block: () -> Unit) {
        if (!isInitialized) {
            isInitialized = true
            block()
        }
    }

    suspend fun perform(dbWrapper: DbWrapper, request: DbRequest):Result<Unit> = withContext(Dispatchers.IO)  {
        try {
            dbWrapper.realmDbStorage.perform(request)
            Result.Success(Unit)
        }catch (e: Exception) {
            Result.Failure(e)
        }
    }
    suspend fun perform(dbWrapper: DbWrapper,writeRequests: List<DbRequest>):Result<Unit> = withContext(Dispatchers.IO)  {
        try {
            dbWrapper.realmDbStorage.perform(writeRequests)
            Result.Success(Unit)
        }catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend inline fun<reified T: Any> perform(dbWrapper: DbWrapper, invalidateRealm: Boolean, request: DbResponseRequest<T>): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.Success(dbWrapper.realmDbStorage.perform(request, invalidateRealm = invalidateRealm))
        }catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
