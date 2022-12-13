package org.zotero.android.architecture.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

open class EventStream<T> {

    private val sharedFlow = MutableSharedFlow<T>()

    fun emit(update: T) {
        sharedFlow.tryEmit(update)
    }

    fun flow(): Flow<T> = sharedFlow.asSharedFlow()
}
