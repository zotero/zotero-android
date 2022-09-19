package org.zotero.android.sync

import com.google.android.play.core.internal.by
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.RealmDbStorage
import org.zotero.android.sync.syncactions.LoadLibraryDataSyncAction
import org.zotero.android.data.AccessPermissions
import timber.log.Timber
import javax.inject.Inject


interface SyncAction<A : Any> {
    suspend fun result(): A
}


class SyncUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val syncRepository: SyncRepository,
    private val sdkPrefs: SdkPrefs,
    private val dbWrapper: DbWrapper
) {

    private var libraryType: LibrarySyncType = LibrarySyncType.all
    private var type: SyncType = SyncType.normal
    private var lastReturnedVersion: Int? = null
    private var conflictRetries: Int = 0
    private var accessPermissions: AccessPermissions? = null

    private var queue = mutableListOf<Action>()
    private var processingAction: Action? = null
    private var createActionOptions: CreateLibraryActionsOptions = CreateLibraryActionsOptions.automatic
    private var didEnqueueWriteActionsToZoteroBackend: Boolean = false
    private var enqueuedUploads: Int = 0
    private var uploadsFailedBeforeReachingZoteroBackend: Int = 0

    private val isSyncing: Boolean
        get() {
            return processingAction != null || queue.isNotEmpty()
        }

    suspend fun start(type: SyncType, libraries: LibrarySyncType) {
        if (isSyncing) {
            return
        }
        this.type = type
        this.libraryType = libraries
        queue.addAll(createInitialActions(libraries = libraries, syncType = type))

        processNextAction()
    }

    private suspend fun processNextAction() {
        if (queue.isEmpty()) {
            processingAction = null
            finish()
            return
        }

        val action = queue.removeFirst()

        if (lastReturnedVersion != null && action.libraryId != processingAction?.libraryId) {
            lastReturnedVersion = null
        }

        processingAction = action

       //TODO requiresConflictReceiver
        process(action = action)
    }

    private suspend fun process(action: Action) {
        when (action) {
            is Action.loadKeyPermissions -> {
                val result = syncRepository.processKeyCheckAction()
                if (result is NetworkResultWrapper.Success) {
                    accessPermissions = result.value
                    processNextAction()
                }
            }
            is Action.createLibraryActions -> {
                processCreateLibraryActions(
                    libraries = action.librarySyncType,
                    options = action.createLibraryActionsOptions
                )
            }

            is Action.syncGroupVersions -> {
                processSyncGroupVersions()
            }
        }


    }

    private suspend fun processCreateLibraryActions(
        libraries: LibrarySyncType,
        options: CreateLibraryActionsOptions
    ) {
        //TODO should use webDavController
        val result = LoadLibraryDataSyncAction(
            type = libraries,
            fetchUpdates = (options != CreateLibraryActionsOptions.forceDownloads),
            loadVersions = (this.type != SyncType.full),
            webDavEnabled = false,
            dbStorage = dbWrapper.realmDbStorage,
            sdkPrefs = sdkPrefs
        ).result()
        //TODO handle errors

        result.subscribe(on: self.workScheduler)
        .subscribe(onSuccess: { [weak self] data in
            self?.finishCreateLibraryActions(with: .success((data, options)))
        }, onFailure: { [weak self] error in
            self?.finishCreateLibraryActions(with: .failure(error))
        })
        .disposed(by: self.disposeBag)

    }

    private suspend fun processSyncGroupVersions() {
        val result = syncRepository.processSyncGroupVersions()
        if (result !is NetworkResultWrapper.Success) {
            //TODO handle abort
            Timber.e((result as NetworkResultWrapper.NetworkError).error.msg)
            return
        }
        val (toUpdate, toRemove) = result.value
        val actions =
            createGroupActions(updateIds = toUpdate, deleteGroups = toRemove, syncType = type)
        finishSyncGroupVersions(actions = actions, updateCount = toUpdate.size)
    }

    private suspend fun finishSyncGroupVersions(actions: List<Action>, updateCount: Int) {
        enqueue(actions = actions, index =  0)
    }

    private suspend fun enqueue(actions: List<Action>, index: Int? = null, delayInSeconds: Int? = null) {
        if (actions.isNotEmpty()) {
            if (index != null) {
                queue.addAll(index, actions)
            } else {
                queue.addAll(actions)
            }
        }

        if (delayInSeconds != null && delayInSeconds > 0) {
            //TODO another delay implementation?
            delay(delayInSeconds * 1000L)
            processNextAction()
        } else {
            processNextAction()
        }
    }

    private fun createGroupActions(
        updateIds: List<Int>,
        deleteGroups: List<Pair<Int, String>>,
        syncType: SyncType
    ): List<Action> {
        var idsToSync:MutableList<Int>
        when (val libraryTypeL = libraryType) {
            is LibrarySyncType.all -> {
                idsToSync = updateIds.toMutableList()
            }
            is LibrarySyncType.specific -> {
                idsToSync = mutableListOf()
                val libraryIds = libraryTypeL.identifiers
                libraryIds.forEach { libraryId ->
                    when (libraryId) {
                        is LibraryIdentifier.group -> {
                            val groupId = libraryId.groupId
                            if (updateIds.contains(groupId)) {
                                idsToSync.add(groupId)
                            }
                        }

                        is LibraryIdentifier.custom -> return@forEach
                    }
                }
            }
        }

        val actions = deleteGroups.map { Action.resolveDeletedGroup(it.first, it.second) }
            .toMutableList<Action>()
        actions.addAll(idsToSync.map { Action.syncGroupToDb(it) })
        val options: CreateLibraryActionsOptions =
            if (syncType == SyncType.full) CreateLibraryActionsOptions.forceDownloads else CreateLibraryActionsOptions.automatic
        actions.add(Action.createLibraryActions (libraryType, options))
        return actions
    }


    private fun finish() {
        //TODO handle reportFinish - handle errors
        cleanup()
    }

    private fun cleanup() {
        processingAction = null
        queue = mutableListOf()
        type = SyncType.normal
        lastReturnedVersion = null
        conflictRetries = 0
        accessPermissions = null
        //TODO batch processor reset
        libraryType = LibrarySyncType.all
        createActionOptions = CreateLibraryActionsOptions.automatic
        didEnqueueWriteActionsToZoteroBackend = false
        enqueuedUploads = 0
        uploadsFailedBeforeReachingZoteroBackend = 0
    }


    private fun createInitialActions(libraries: LibrarySyncType, syncType: SyncType): List<Action> {
        when (libraries) {
            is LibrarySyncType.all ->
                return listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
            is LibrarySyncType.specific -> {
                for (identifier in libraries.identifiers) {
                    if (identifier is LibraryIdentifier.group) {
                        return listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
                    }
                }
                val options = if (syncType == SyncType.full)
                    CreateLibraryActionsOptions.forceDownloads else
                    CreateLibraryActionsOptions.automatic
                return listOf(
                    Action.loadKeyPermissions,
                    Action.createLibraryActions(libraries, options)
                )

            }
        }
    }
}
