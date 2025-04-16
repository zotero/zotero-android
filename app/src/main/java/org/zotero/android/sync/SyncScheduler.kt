package org.zotero.android.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import timber.log.Timber
import java.lang.Integer.min
import java.util.Date
import java.util.Timer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.timerTask

@Singleton
class SyncScheduler @Inject constructor(
    val syncController: SyncUseCase,
    val observable: SyncObservableEventStream,
    val dispatchers: Dispatchers,
) {
    data class Sync(
        val type: SyncKind,
        val libraries: Libraries,
        val retryAttempt: Int = 0,
        val retryOnce: Boolean = false,
    )

    var inProgress = MutableStateFlow<Boolean>(false)

    private val coroutineScope = CoroutineScope(dispatchers.io)
    private val syncSchedulerSemaphore = Semaphore(1)

    private val syncTimeout = 3_000L
    private val fullSyncTimeout = 3600_000
    private lateinit var retryIntervals: MutableList<Int>

    private var syncInProgress: Sync? = null
    private lateinit var syncQueue: MutableList<Sync>

    private lateinit var lastSyncFinishDate: Date
    private lateinit var lastFullSyncDate: Date

    private val canPerformFullSync: Boolean
        get() {
            return Date().time - this.lastFullSyncDate.time > this.fullSyncTimeout
        }

    private var timer: Timer? = null

    fun init(retryIntervals: List<Int>) {
        this.retryIntervals = retryIntervals.toMutableList()
        this.inProgress.tryEmit(false)
        this.syncQueue = mutableListOf()
        this.lastSyncFinishDate = Date(0)
        this.lastFullSyncDate = Date(0)
        observable.flow()
            .onEach { sync ->
                syncSchedulerSemaphore.withPermit {
                    this.syncInProgress = null
                    this.lastSyncFinishDate = Date()

                    if (sync != null) {
                        enqueueAndStart(sync = sync)
                    } else {
                        startNextSync()
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun request(type: SyncKind, libraries: Libraries) {
        coroutineScope.launch {
            syncSchedulerSemaphore.withPermit {
                Timber.i("SyncScheduler: requested $type sync for $libraries")
                enqueueAndStart(sync = Sync(type = type, libraries = libraries))
            }
        }

    }

    fun webSocketUpdate(libraryId: LibraryIdentifier) {
        Timber.i("SyncScheduler: websocket sync for ${libraryId}")
        coroutineScope.launch {
            syncSchedulerSemaphore.withPermit {
                enqueueAndStart(
                    sync = Sync(
                        type = SyncKind.normal,
                        libraries = Libraries.specific(listOf(libraryId))
                    )
                )
            }

        }

    }

    fun cancelSync() {
        Timber.i("SyncScheduler: cancel sync")
        this.syncController.cancel()
        resetTimer()
        this.syncInProgress = null
        this.syncQueue = mutableListOf()
    }

    private fun enqueueAndStart(sync: Sync) {
        enqueue(sync = sync)
        startNextSync()
    }

    private fun enqueue(sync: Sync) {
        if (sync.type == SyncKind.full && sync.libraries == Libraries.all) {
            if (!this.canPerformFullSync) {
                return
            }
            Timber.i("SyncScheduler: clean queue, enqueue full sync")
            this.syncQueue = mutableListOf(sync)
            resetTimer()
        } else if (this.syncQueue.isEmpty()) {
            this.syncQueue.add(sync)
        } else if (sync.retryAttempt > 0) {
            Timber.i("SyncScheduler: enqueue retry sync #${sync.retryAttempt}; queue count = ${this.syncQueue.size}")
            val index = this.syncQueue.indexOfFirst { it.retryAttempt == 0 }
            if (index != -1) {
                this.syncQueue.add(element = sync, index = index)
            } else {
                this.syncQueue.add(sync)
            }
        } else if (!this.syncQueue.any { it.type == sync.type && it.libraries == sync.libraries }) {
            // New sync request should be added to the end of queue if it's not a duplicate
            this.syncQueue.add(sync)
        }
    }

    private fun startNextSync() {
        if (this.syncInProgress != null) {
            return
        }
        val nextSync = this.syncQueue.firstOrNull()
        if (nextSync == null) {
            this.inProgress.tryEmit(false)
            return
        }

        resetTimer()

        val delay: Long
        if (nextSync.retryAttempt > 0) {
            val index = min(nextSync.retryAttempt, this.retryIntervals.size)
            delay = this.retryIntervals[index - 1].toLong()
        } else {
            delay = this.syncTimeout
        }

        val timeSinceLastSync = Date().time - this.lastSyncFinishDate.time
        if (timeSinceLastSync < delay) {
            Timber.i("SyncScheduler: delay sync for ${delay - timeSinceLastSync}")
            delayNextSync(delay - timeSinceLastSync)
            return
        }

        if (!this.inProgress.value) {
            this.inProgress.tryEmit(true)
        }

        Timber.i("SyncScheduler: start ${nextSync.type} sync for ${nextSync.libraries}")
        this.syncQueue.removeFirst()
        this.syncInProgress = nextSync
        startSyncController(nextSync)
    }

    private fun startSyncController(nextSync: Sync) {
        coroutineScope.launch {
            syncSchedulerSemaphore.withPermit {
                this@SyncScheduler.syncController.start(
                    type = nextSync.type,
                    libraries = nextSync.libraries,
                    retryAttempt = nextSync.retryAttempt,
                    syncSchedulerSemaphore = syncSchedulerSemaphore,
                )
            }

        }
    }

    private fun delayNextSync(timeout: Long) {
        if (this.syncInProgress != null) {
            return
        }

        timer?.schedule(timerTask {
            coroutineScope.launch {
                syncSchedulerSemaphore.withPermit {
                    startNextSync()
                }
            }

        }, timeout)
    }

    private fun resetTimer() {
        timer?.cancel()
        timer = Timer()
    }

    fun startSyncController(type: SyncKind, libraries: Libraries, retryAttempt: Int,) {
        coroutineScope.launch {
            syncSchedulerSemaphore.withPermit {
                this@SyncScheduler.syncController.start(
                    type = type,
                    libraries = libraries,
                    retryAttempt = retryAttempt,
                    syncSchedulerSemaphore = syncSchedulerSemaphore,
                )
            }
        }
    }
}

@Singleton
class SyncObservableEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<SyncScheduler.Sync?>(applicationScope)