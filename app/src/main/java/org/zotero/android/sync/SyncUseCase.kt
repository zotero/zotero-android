package org.zotero.android.sync

import io.realm.exceptions.RealmError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.zotero.android.api.network.NetworkResultWrapper
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.data.AccessPermissions
import org.zotero.android.sync.syncactions.LoadLibraryDataSyncAction
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
    private var createActionOptions: CreateLibraryActionsOptions =
        CreateLibraryActionsOptions.automatic
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
                } else {
                    val networkError = result as NetworkResultWrapper.NetworkError
                    val er = syncError(
                        networkError = networkError, data = SyncError.ErrorData.from(
                            libraryId = LibraryIdentifier.custom(
                                RCustomLibraryType.myLibrary
                            )
                        )
                    ).fatal2S ?: SyncError.Fatal.permissionLoadingFailed
                    abort(er)
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
        try {
            //TODO should use webDavController
            val result = LoadLibraryDataSyncAction(
                type = libraries,
                fetchUpdates = (options != CreateLibraryActionsOptions.forceDownloads),
                loadVersions = (this.type != SyncType.full),
                webDavEnabled = false,
                dbStorage = dbWrapper.realmDbStorage,
                sdkPrefs = sdkPrefs
            ).result()
            finishCreateLibraryActions(pair = result to options)
        } catch (e: Exception) {
            finishCreateLibraryActions(exception = e)
        }
    }

    private suspend fun finishCreateLibraryActions(
        exception: Exception? = null,
        pair: Pair<List<LibraryData>, CreateLibraryActionsOptions>? = null
    ) {

        if (exception != null) {
            val er = syncError(
                error = exception, data = SyncError.ErrorData.from(
                    libraryId = LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            ).fatal2S ?: SyncError.Fatal.allLibrariesFetchFailed
            abort(er)
            return
        }

        var libraryNames: Map<LibraryIdentifier, String>? = null
        val (data, options) = pair!!
        if (options == CreateLibraryActionsOptions.automatic || this.type == SyncType.full) {
            var nameDictionary = mutableMapOf<LibraryIdentifier, String>()
            for (libraryData in data) {
                nameDictionary[libraryData.identifier] = libraryData.name
            }
            libraryNames = nameDictionary
        }
        val (actions, queueIndex, writeCount) = createLibraryActions(
            data,
            options
        )
        this.didEnqueueWriteActionsToZoteroBackend =
            options != CreateLibraryActionsOptions.automatic || writeCount > 0

        this.createActionOptions = options
        val names = libraryNames
        if (names != null) {
            //TODO update progress
        }
        enqueue(actions = actions, index = queueIndex)

    }

    private suspend fun processSyncGroupVersions() {
        val result = syncRepository.processSyncGroupVersions()
        if (result !is NetworkResultWrapper.Success) {
            Timber.e((result as NetworkResultWrapper.NetworkError).error.msg)
            val er = syncError(
                networkError = result, data = SyncError.ErrorData.from(
                    libraryId = LibraryIdentifier.custom(
                        RCustomLibraryType.myLibrary
                    )
                )
            ).fatal2S ?: SyncError.Fatal.groupSyncFailed
            abort(er)
            return
        }
        val (toUpdate, toRemove) = result.value
        val actions =
            createGroupActions(updateIds = toUpdate, deleteGroups = toRemove, syncType = type)
        finishSyncGroupVersions(actions = actions, updateCount = toUpdate.size)
    }

    private suspend fun finishSyncGroupVersions(actions: List<Action>, updateCount: Int) {
        enqueue(actions = actions, index = 0)
    }

    private suspend fun enqueue(
        actions: List<Action>,
        index: Int? = null,
        delayInSeconds: Int? = null
    ) {
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
        var idsToSync: MutableList<Int>
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
        actions.add(Action.createLibraryActions(libraryType, options))
        return actions
    }


    private fun finish() {
        //TODO handle reportFinish - handle errors
        cleanup()
    }

    private fun abort(error: SyncError.Fatal) {
        Timber.i("Sync: aborted")
        Timber.i("Error: $error")

        //TODO display error
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

    private fun createLibraryActions(
        data: List<LibraryData>,
        creationOptions: CreateLibraryActionsOptions
    ): Triple<List<Action>, Int?, Int> {
        var writeCount = 0
        var allActions = mutableListOf<Action>()

        for (libraryData in data) {
            val libraryId = libraryData.identifier

            when (creationOptions) {
                CreateLibraryActionsOptions.forceDownloads ->
                    createDownloadActions(libraryId, versions = libraryData.versions).forEach {
                        allActions.add(it)
                    }

                CreateLibraryActionsOptions.onlyWrites, CreateLibraryActionsOptions.automatic -> {
                    if (!libraryData.updates.isEmpty() || (!libraryData.deletions.isEmpty()) || libraryData.hasUpload) {
                        when (libraryData.identifier) {
                            is LibraryIdentifier.group -> {
                                // We need to check permissions for group
                                if (libraryData.canEditMetadata) {
                                    val actions = this.createUpdateActions(
                                        updates = libraryData.updates,
                                        deletions = libraryData.deletions,
                                        libraryId = libraryId
                                    )
                                    writeCount += actions.count() - 1
                                    actions.forEach {
                                        allActions.add(it)

                                    }
                                } else {
                                    allActions.add(
                                        Action.resolveGroupMetadataWritePermission(
                                            libraryData.identifier.groupId,
                                            libraryData.name
                                        )
                                    )
                                }
                            }
                            is LibraryIdentifier.custom -> {
                                val actions = createUpdateActions(
                                    updates = libraryData.updates,
                                    deletions = libraryData.deletions,
                                    libraryId = libraryId
                                )
                                writeCount += actions.count() - 1
                                // We can always write to custom libraries
                                actions.forEach {
                                    allActions.add(it)

                                }

                            }

                        }
                    } else if (creationOptions == CreateLibraryActionsOptions.automatic) {
                        createDownloadActions(libraryId, versions = libraryData.versions).forEach {
                            allActions.add(it)
                        }
                    }

                    if (libraryData.hasWebDavDeletions) {
                        allActions.add(Action.performWebDavDeletions(libraryId))
                    }
                }
            }
        }

        // Forced downloads or writes are pushed to the beginning of the queue, because only currently running action
        // can force downloads or writes
        val index: Int? = if (creationOptions == CreateLibraryActionsOptions.automatic) null else 0
        return Triple(allActions, index, writeCount)
    }

    private fun createDownloadActions(
        libraryId: LibraryIdentifier,
        versions: Versions
    ): List<Action> {
        when (this.type) {
            SyncType.collectionsOnly ->
                return listOf(
                    Action.syncVersions(
                        libraryId = libraryId,
                        objectS = SyncObject.collection,
                        version = versions.collections, checkRemote = true
                    )
                )

            SyncType.full ->
                return listOf(
                    Action.syncSettings(libraryId, versions.settings),
                    Action.syncDeletions(libraryId, versions.deletions),
                    Action.storeDeletionVersion(
                        libraryId = libraryId,
                        version = versions.deletions
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.collection,
                        version = versions.collections, checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.search,
                        version = versions.searches, checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.item,
                        version = versions.items,
                        checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        objectS = SyncObject.trash,
                        version = versions.trash,
                        checkRemote = true
                    )
                )

            SyncType.ignoreIndividualDelays, SyncType.normal ->
                return listOf(
                    Action.syncSettings(libraryId, versions.settings),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.collection,
                        version = versions.collections, checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.search,
                        version = versions.searches, checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        SyncObject.item,
                        version = versions.items,
                        checkRemote = true
                    ),
                    Action.syncVersions(
                        libraryId = libraryId,
                        objectS = SyncObject.trash,
                        version = versions.trash,
                        checkRemote = true
                    ),
                    Action.syncDeletions(libraryId, versions.deletions),
                    Action.storeDeletionVersion(libraryId = libraryId, version = versions.deletions)
                )
        }
    }

    private fun createUpdateActions(
        updates: List<WriteBatch>,
        deletions: List<DeleteBatch>,
        libraryId: LibraryIdentifier
    ): List<Action> {
        var actions = mutableListOf<Action>()
        if (!updates.isEmpty()) {
            updates.forEach {
                actions.add(Action.submitWriteBatch(it))
            }
        }
        if (!deletions.isEmpty()) {
            deletions.forEach {
                actions.add(Action.submitDeleteBatch(it))
            }
        }
        actions.add(
            Action.createUploadActions(
                libraryId = libraryId,
                hadOtherWriteActions = (!updates.isEmpty() || !deletions.isEmpty())
            )
        )
        return actions
    }


    private fun syncError(
        error: Throwable? = null,
        networkError: NetworkResultWrapper.NetworkError? = null,
        data: SyncError.ErrorData
    ): SyncError {
        val fatalError = error as? SyncError.Fatal
        if (fatalError != null) {
            return SyncError.fatal2(error)
        }

        val nonFatalError = error as? SyncError.NonFatal
        if (nonFatalError != null) {
            return SyncError.nonFatal2(error)
        }

        val syncActionError = error as? SyncActionError

        if (syncActionError != null) {
            when (syncActionError) {
                is SyncActionError.attachmentAlreadyUploaded, is SyncActionError.attachmentItemNotSubmitted ->
                    return SyncError.nonFatal2(
                        SyncError.NonFatal.unknown(
                            syncActionError.localizedMessage ?: ""
                        )
                    )
                is SyncActionError.attachmentMissing ->
                    return SyncError.nonFatal2(
                        SyncError.NonFatal.attachmentMissing(
                            key = syncActionError.key,
                            libraryId = syncActionError.libraryId,
                            title = syncActionError.title
                        )
                    )
                is SyncActionError.annotationNeededSplitting ->
                    return SyncError.nonFatal2(
                        SyncError.NonFatal.annotationDidSplit(
                            messageS = syncActionError.messageS,
                            libraryId = syncActionError.libraryId
                        )
                    )
                is SyncActionError.submitUpdateFailures ->
                    return SyncError.nonFatal2(SyncError.NonFatal.unknown(syncActionError.messages))
            }
        }

        // TODO handle web dav errors

        //TODO handle reportMissing
        if (networkError?.error?.isUnchanged() == true) {
            return SyncError.nonFatal2(SyncError.NonFatal.unchanged)
        }


        if (networkError != null) {
            return networkErrorRequiresAbort(
                networkError,
                response = networkError.error.msg,
                data = data
            )
        }

        // Check realm errors, every "core" error is bad. Can't create new Realm instance, can't continue with sync
        if (error is RealmError) {
            Timber.e("received realm error - $error")
            return SyncError.fatal2(SyncError.Fatal.dbError)
        }
        val schemaError = error as? SchemaError
        if (schemaError != null) {
            return SyncError.nonFatal2(SyncError.NonFatal.schema(error))
        }
        val parsingError = error as? Parsing.Error
        if (parsingError != null) {
            return SyncError.nonFatal2(SyncError.NonFatal.parsing(error))
        }
        Timber.e("received unknown error - $error")
        return SyncError.nonFatal2(SyncError.NonFatal.unknown(error?.localizedMessage ?: ""))
    }

    private fun networkErrorRequiresAbort(
        error: NetworkResultWrapper.NetworkError,
        response: String,
        data: SyncError.ErrorData
    ): SyncError {
        val responseMessage: () -> String = {
            if (response == "No Response") {
                error.error.msg
            } else {
                response
            }
        }

        val code = error.error.code
        when (code) {
            304 ->
                return SyncError.nonFatal2(SyncError.NonFatal.unchanged)
            413 ->
                return SyncError.nonFatal2(SyncError.NonFatal.quotaLimit(data.libraryId))
            507 ->
                return SyncError.nonFatal2(SyncError.NonFatal.insufficientSpace)
            503 ->
                return SyncError.fatal2(SyncError.Fatal.serviceUnavailable)
            else -> {
                if (code >= 400 && code <= 499 && code != 403) {
                    return SyncError.fatal2(
                        SyncError.Fatal.apiError(
                            response = responseMessage(),
                            data = data
                        )
                    )
                } else {
                    return SyncError.nonFatal2(
                        SyncError.NonFatal.apiError(
                            response = responseMessage(),
                            data = data
                        )
                    )
                }
            }
        }
    }
}
