package org.zotero.android.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.CleanupUnusedTags
import org.zotero.android.database.requests.InitializeCustomLibrariesDbRequest
import org.zotero.android.websocket.APIWebSocketController
import org.zotero.android.websocket.ChangeWsResponse
import org.zotero.android.websocket.ChangeWsResponseKindEventStream
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserControllers @Inject constructor(
    dispatcher: CoroutineDispatcher,
    private val dbWrapperMain: DbWrapperMain,
    private val syncController: SyncUseCase,
    private val syncScheduler: SyncScheduler,
    private val objectUserChangeEventStream: ObjectUserChangeEventStream,
    private val applicationScope: ApplicationScope,
    private val dispatchers: Dispatchers,
    private val webSocketController: APIWebSocketController,
    private val changeWsResponseKindEventStream: ChangeWsResponseKindEventStream,
    private val fileDownloader: AttachmentDownloader,
    private val defaults: Defaults,
) {

    private lateinit var changeObserver: ObjectUserChangeObserver
    private var isFirstLaunch: Boolean = true

    private var coroutineScope = CoroutineScope(dispatcher)
    private var runningSyncJob: Job? = null

    var isControllerInitialized: Boolean = false

    fun init(userId: Long, sessionId: String?) {
        createDbStorage(userId = userId, sessionId = sessionId)
        syncController.init(
            userId = userId,
            syncDelayIntervals = DelayIntervals.sync,
            maxRetryCount = DelayIntervals.retry.size,
        )
        fileDownloader.init(userId = userId)
        var isFirstLaunch = false
        coroutineScope.launch {
            dbWrapperMain.realmDbStorage.perform(coordinatorAction = { coordinator ->
                isFirstLaunch = coordinator.perform(InitializeCustomLibrariesDbRequest())
                coordinator.perform(CleanupUnusedTags())
                coordinator.invalidate()
            })
        }


        this.isFirstLaunch = isFirstLaunch
        syncScheduler.init(DelayIntervals.retry)
        this.changeObserver = ObjectUserChangeObserver(
            dbWrapperMain = dbWrapperMain,
            observable = objectUserChangeEventStream,
            applicationScope = applicationScope,
            dispatchers = dispatchers
        )
        //TODO RItemLocaleController
        runningSyncJob?.cancel()

        isControllerInitialized = true
    }

    fun enableSync(apiKey: String) {
        Timber.i("UserControllers: performFullSyncGuard: ${defaults.performFullSyncGuard()}; currentPerformFullSyncGuard: ${defaults.currentPerformFullSyncGuard}")
        if (defaults.performFullSyncGuard() < defaults.currentPerformFullSyncGuard) {
            defaults.setDidPerformFullSyncFix(false)
            defaults.setPerformFullSyncGuard(defaults.currentPerformFullSyncGuard)
        } else {
            defaults.setDidPerformFullSyncFix(true)
        }
        Timber.i("UserControllers: didPerformFullSyncFix: ${defaults.didPerformFullSyncFix()}")

        objectUserChangeEventStream.flow()
            .debounce(3000)
            .onEach { changedLibraries ->
            syncScheduler.request(
                type = SyncKind.normal,
                libraries = Libraries.specific(changedLibraries),
            )
        }.launchIn(applicationScope)

        changeWsResponseKindEventStream.flow()
            .debounce(3000)
            .onEach { change ->
            when (change) {
                is ChangeWsResponse.Kind.translators -> {
                    // TODO update translations
                }
                is ChangeWsResponse.Kind.library -> {
                    syncScheduler.webSocketUpdate(libraryId = change.libraryIdentifier)
                }
            }
        }.launchIn(applicationScope)
        this.webSocketController.init()
        this.webSocketController.connect(subscriptionValue = apiKey, completed = ::onWebSocketConnectionEstablished)

    }

    private fun onWebSocketConnectionEstablished() {
        //TODO backgroundUploadObserver.updateSessions()
        val type = if (defaults.didPerformFullSyncFix()) {
            SyncKind.normal
        } else {
            SyncKind.full
        }
        this.syncScheduler.request(type = type, libraries = Libraries.all)
    }

    fun disableSync(apiKey: String?) {
        this.syncScheduler.cancelSync()
        this.webSocketController.disconnect(subscriptionValue = apiKey)
        runningSyncJob?.cancel()
    }

    private fun createDbStorage(userId: Long, sessionId: String?) {
        dbWrapperMain.initWithMainConfiguration(userId, sessionId)
    }

}