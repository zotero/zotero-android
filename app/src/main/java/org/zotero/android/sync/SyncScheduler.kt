package org.zotero.android.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class SchedulerAction (val syncType: SyncType, val librarySyncType: LibrarySyncType)

@Singleton
class SyncScheduler @Inject constructor(
    val syncController: SyncUseCase,
    val observable: SyncObservableEventStream,
    val dispatcher: CoroutineDispatcher
) {
    private var coroutineScope = CoroutineScope(dispatcher)
    private var runningJob: Job? = null

    private val timeout = 3_000L
    private val fullSyncTimeout = 3600_000

    private var inProgress: SchedulerAction? = null
    private var nextAction: SchedulerAction? = null

    private var lastSyncDate: Date? = null
    private var lastFullSyncDate: Date? = null

    private val canPerformFullSync: Boolean get() {
        val date = this.lastFullSyncDate
        if (date == null) {
            return true
        }
        return Date().time - date.time <= this.fullSyncTimeout
    }

    init {

        observable.flow()
            .onEach { data ->
                try {
                    this.inProgress = null
                    if (data != null) {
                        _enqueueAndStartTimer(action = data)
                    } else if (this.nextAction != null) {
                        startTimer()
                    }
                } catch (e: Exception) {
                   Timber.e(e)
                    this.inProgress = null
                    if (this.nextAction != null) {
                        startTimer()
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun startNextAction() {
        val nextAction = this.nextAction
        if (this.inProgress != null || nextAction == null) {
            return
        }
        val (syncType, librarySyncType) = nextAction
        this.inProgress = this.nextAction
        this.nextAction = null
        this.lastSyncDate = Date()
        if (syncType == SyncType.full) {
            this.lastFullSyncDate = this.lastSyncDate
        }

        this@SyncScheduler.syncController.start(type = syncType, libraries = librarySyncType)
    }

    private fun enqueueAndStart(action: SchedulerAction) {
        _enqueueAndStart(action = action)
    }

    private fun _enqueueAndStart(action: SchedulerAction) {
        if (action.syncType == SyncType.full && !this.canPerformFullSync) {
            return
        }
        enqueue(action = action)
        startNextAction()
    }

    private fun enqueueAndStartTimer(action: SchedulerAction) {
        _enqueueAndStartTimer(action = action)
    }

    private fun _enqueueAndStartTimer(action: SchedulerAction) {
        if (action.syncType == SyncType.full && !this.canPerformFullSync) {
            return
        }
        enqueue(action = action)
        startTimer()
    }

    private fun enqueue(action: SchedulerAction) {
        val nextAction = this.nextAction
        if (nextAction == null) {
            this.nextAction = action
            return
        }
        val (nextSyncType, nextLibrarySyncType) = nextAction

        val type = if(!compareSyncTypesLess(action.syncType, nextSyncType)) nextSyncType else action.syncType
        val a = nextLibrarySyncType
        val b = action.librarySyncType
        when {
            a == LibrarySyncType.all && b == LibrarySyncType.all ->
                this.nextAction = SchedulerAction(type, LibrarySyncType.all)
            a is LibrarySyncType.specific && b == LibrarySyncType.all ->
                this.nextAction = SchedulerAction(type, LibrarySyncType.all)
            a is LibrarySyncType.specific && b is LibrarySyncType.specific -> {
                val nextIds = a.identifiers
                val newIds = b.identifiers
                val unionedIds = (nextIds.toSet().union(newIds.toSet())).toList()
                this.nextAction = SchedulerAction(type, LibrarySyncType.specific(unionedIds))
            }
            a == LibrarySyncType.all && b is LibrarySyncType.specific -> {
                //no-op
            }
        }
    }

    private fun startTimer() {
        if (this.inProgress != null) {
            return
        }
        runningJob = coroutineScope.launch {
            delay(this@SyncScheduler.timeout)
            startNextAction()
        }
    }

    fun request(type: SyncType, libraries: LibrarySyncType) {
        request(type = type, libraries = libraries, applyDelay = false)
    }

    fun request(type: SyncType, libraries: LibrarySyncType, applyDelay: Boolean) {
        if (applyDelay) {
            this.enqueueAndStartTimer(action = SchedulerAction(type, libraries))
        } else {
            this.enqueueAndStart(action = SchedulerAction(type, libraries))
        }
    }

    fun webSocketUpdate(libraryId: LibraryIdentifier) {
        this.enqueueAndStartTimer(action = SchedulerAction(SyncType.normal, LibrarySyncType.specific(
            listOf(libraryId))))
    }

    fun cancelSync() {
        runningJob?.cancel()
        this.syncController.cancel()
        this.inProgress = null
        this.nextAction = null
    }

    private fun compareSyncTypesLess(lhs: SyncType, rhs: SyncType): Boolean {
        when {
            (lhs == SyncType.collectionsOnly && rhs == SyncType.normal)
                    || (lhs == SyncType.collectionsOnly && rhs == SyncType.ignoreIndividualDelays)
                    || (lhs == SyncType.collectionsOnly && rhs == SyncType.full)
                    || (lhs == SyncType.normal && rhs == SyncType.ignoreIndividualDelays)
                    || (lhs == SyncType.normal && rhs == SyncType.full)
                    || (lhs == SyncType.ignoreIndividualDelays && rhs == SyncType.full) ->
                return true
            else ->
                return false
        }
    }

}

@Singleton
class SyncObservableEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<SchedulerAction?>(applicationScope)