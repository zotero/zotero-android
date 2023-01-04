package org.zotero.android.sync

import InitializeCustomLibrariesDbRequest
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.CleanupUnusedTags
import org.zotero.android.files.FileStore
import org.zotero.android.websocket.ChangeWsResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserControllers @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val fileStore: FileStore,
    private val dbWrapper: DbWrapper,
    private val context: Context,
    private val syncController: SyncUseCase,
    private val syncScheduler: SyncScheduler,
    private val objectUserChangeEventStream: ObjectUserChangeEventStream,
    private val applicationScope: ApplicationScope,
    private val dispatchers: Dispatchers,
    private val webSocketController: WebSocketController,
    private val changeWsResponseKindEventStream: ChangeWsResponseKindEventStream
) {

    private lateinit var changeObserver: ObjectUserChangeObserver
    private var isFirstLaunch: Boolean = true

    private var coroutineScope = CoroutineScope(dispatcher)
    private var runningSyncJob: Job? = null

    var isControllerInitialized: Boolean = false

    fun init(userId: Long, controllers: Controllers) {
        createDbStorage(userId)
        syncController.init(
            userId = userId,
            syncDelayIntervals = DelayIntervals.sync,
            conflictDelays = DelayIntervals.conflict
        )
        var isFirstLaunch = false
        coroutineScope.launch {
            dbWrapper.realmDbStorage.perform(coordinatorAction = { coordinator ->
                isFirstLaunch = coordinator.perform(InitializeCustomLibrariesDbRequest())
                coordinator.perform(CleanupUnusedTags())
            })
        }


        this.isFirstLaunch = isFirstLaunch
        this.changeObserver = ObjectUserChangeObserver(
            dbWrapper = dbWrapper, observable = objectUserChangeEventStream,
            applicationScope = applicationScope,
            dispatchers = dispatchers
        )
        //TODO RItemLocaleController
        runningSyncJob?.cancel()

        isControllerInitialized = true
    }

    fun enableSync(apiKey: String) {
        objectUserChangeEventStream.flow().onEach { changedLibraries ->
            syncScheduler.request(
                type = SyncType.normal,
                libraries = LibrarySyncType.specific(changedLibraries),
                applyDelay = true
            )
        }.launchIn(applicationScope)

        changeWsResponseKindEventStream.flow().onEach { change ->
            when (change) {
                is ChangeWsResponse.Kind.translators -> {
                    // TODO update translations
                }
                is ChangeWsResponse.Kind.library -> {
                    syncScheduler.webSocketUpdate(libraryId = change.libraryIdentifier)
                }
            }
        }.launchIn(applicationScope)

        this.webSocketController.connect(apiKey = apiKey, completed = {
            this.syncScheduler.request(type = SyncType.normal, libraries = LibrarySyncType.all)
        })

    }

    fun disableSync(apiKey: String?) {
        this.syncScheduler.cancelSync()
        this.webSocketController.disconnect(apiKey = apiKey)
        runningSyncJob?.cancel()
    }

    private fun createDbStorage(userId: Long) {
        val file = fileStore.dbFile(userId)
        dbWrapper.initWithMainConfiguration(context = context, dbFile = file)
    }
}