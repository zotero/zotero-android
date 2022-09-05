package org.zotero.android.sync

import kotlinx.coroutines.CoroutineDispatcher
import org.zotero.android.data.AccessPermissions
import javax.inject.Inject

class SyncUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val syncRepository: SyncRepository
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
                syncRepository.processKeyCheckAction()
            }
            is Action.createLibraryActions -> {

            }

            is Action.syncGroupVersions -> {
                syncRepository.processSyncGroupVersions()
            }
        }


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
