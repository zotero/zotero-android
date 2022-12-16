package org.zotero.android.architecture.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.ApplicationScope

open class EventStream<T>(private val applicationScope: ApplicationScope) {

    private val sharedFlow = MutableSharedFlow<T>()

    fun emit(update: T) {
        sharedFlow.tryEmit(update)
    }

    fun emitAsync(update: T) {
        applicationScope.launch {
            sharedFlow.emit(update)
        }
    }

    fun flow(): Flow<T> = sharedFlow.asSharedFlow()
}
