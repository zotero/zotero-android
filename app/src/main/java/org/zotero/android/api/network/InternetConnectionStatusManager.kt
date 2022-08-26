package org.zotero.android.api.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import timber.log.Timber

class InternetConnectionStatusManager constructor(
    initialStatus: InternetConnectionStatus
) {

    private val stateFlow = MutableStateFlow(initialStatus)

    var status: InternetConnectionStatus
        get() = stateFlow.value
        set(value) {
            Timber.d("Updating Status=$value")
            stateFlow.tryEmit(value)
        }

    fun flow(): Flow<InternetConnectionStatus> = stateFlow

    internal suspend fun awaitUntilConnected() = flow()
        .filter { it == InternetConnectionStatus.CONNECTED }
        .take(1)
        .single()
}
