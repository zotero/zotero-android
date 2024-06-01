package org.zotero.android.architecture.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.ApplicationScope

open class StateEventStream<T>(private val applicationScope: ApplicationScope, initValue: T?) {

    private val sharedFlow = MutableStateFlow(initValue)

    fun emit(update: T?) {
        sharedFlow.tryEmit(update)
    }

    fun emitAsync(update: T) {
        applicationScope.launch {
            sharedFlow.emit(update)
        }
    }

    fun flow(): StateFlow<T?> = sharedFlow.asStateFlow()

    fun currentValue(): T? {
        return sharedFlow.value
    }
}
