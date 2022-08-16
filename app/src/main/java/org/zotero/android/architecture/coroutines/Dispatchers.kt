package org.zotero.android.architecture.coroutines

import kotlinx.coroutines.CoroutineDispatcher

interface Dispatchers {
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
    val io: CoroutineDispatcher
}

class QDispatchers : Dispatchers {
    override val default = kotlinx.coroutines.Dispatchers.Default
    override val main = kotlinx.coroutines.Dispatchers.Main
    override val unconfined = kotlinx.coroutines.Dispatchers.Unconfined
    override val io = kotlinx.coroutines.Dispatchers.IO
}
