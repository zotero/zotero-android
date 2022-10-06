package org.zotero.android.sync

import io.realm.exceptions.RealmError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.data.AccessPermissions
import org.zotero.android.data.mappers.ItemResponseMapper
import org.zotero.android.files.FileStore
import org.zotero.android.sync.syncactions.LoadLibraryDataSyncAction
import org.zotero.android.sync.syncactions.SyncVersionsSyncAction
import timber.log.Timber
import java.lang.Integer.min
import javax.inject.Inject


interface SyncAction<A : Any> {
    suspend fun result(): A
}


class SyncUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val syncRepository: SyncRepository,
    private val sdkPrefs: SdkPrefs,
    private val dbWrapper: DbWrapper,
    private val syncApi: SyncApi,
    private val fileStore: FileStore,
    private val itemResponseMapper: ItemResponseMapper,
    private val itemResultsUseCase: ItemResultsUseCase
) {

    private var coroutineScope = CoroutineScope(dispatcher)

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

    private var syncDelayIntervals: List<Double> = DelayIntervals.sync

    private var nonFatalErrors: MutableList<SyncError.NonFatal> = mutableListOf()

    private val isSyncing: Boolean
        get() {
            return processingAction != null || queue.isNotEmpty()
        }

    private var batchProcessor: SyncBatchProcessor? = null

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
                if (result is CustomResult.GeneralSuccess) {
                    accessPermissions = result.value
                    processNextAction()
                } else {
                    val customResultError = result as CustomResult.GeneralError
                    val er = syncError(
                        customResultError = customResultError, data = SyncError.ErrorData.from(
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
            is Action.syncVersions -> {
                processSyncVersions(
                    libraryId = action.libraryId,
                    objectS = action.objectS,
                    version = action.version,
                    checkRemote = action.checkRemote
                )
            }
            //TODO implement other actions

            is Action.syncBatchesToDb -> {
                processBatchesSync(action.batches)
            }
            else -> {
                processNextAction()
            }
        }

    }

    private suspend fun processBatchesSync(batches: List<DownloadBatch>) {
        val batch = batches.firstOrNull()
        if (batch == null) {
            processNextAction()
            return
        }

        val libraryId = batch.libraryId
        val objectS = batch.objectS

        this.batchProcessor =
            SyncBatchProcessor(batches = batches, userId = sdkPrefs.getUserId(), syncApi = syncApi,
                dbWrapper = this.dbWrapper, fileStore = this.fileStore,
                itemResponseMapper = itemResponseMapper,
                completion = { result ->
                    this.batchProcessor = null
                    finishBatchesSyncAction(libraryId, objectS = objectS, result = result)
                }, itemResultsUseCase = itemResultsUseCase
            )
        this.batchProcessor?.start()
    }

    private fun finishBatchesSyncAction(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        result: CustomResult<SyncBatchResponse>
    ) {
        when (result) {
            is CustomResult.GeneralSuccess -> {
                //TODO handle non fatal errors
                val (failedKeys, parseErrors) = result.value

                if (failedKeys.isEmpty()) {
                    coroutineScope.launch {
                        processNextAction()
                    }
                } else {
                    //TODO mark for resync
                }
            }
            is CustomResult.GeneralError -> {
                val syncError = this.syncError(
                    customResultError = result,
                    data = SyncError.ErrorData.from(libraryId = libraryId)
                )
                when (syncError) {
                    is SyncError.fatal2 ->
                        abort(error = syncError.error)
                    is SyncError.nonFatal2 ->
                        handleNonFatal(error = syncError.error, libraryId = libraryId, version = null)
                }
            }
        }
    }

    private suspend fun processSyncVersions(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        version: Int,
        checkRemote: Boolean
    ) {
        val lastVersion = this.lastReturnedVersion

        try {
            val (newVersion, toUpdate) = SyncVersionsSyncAction(
                objectS = objectS,
                sinceVersion = version,
                currentVersion = lastVersion,
                syncType = this.type,
                libraryId = libraryId,
                userId = sdkPrefs.getUserId(),
                syncDelayIntervals = this.syncDelayIntervals,
                checkRemote = checkRemote,
                syncApi = this.syncApi,
                dbWrapper = this.dbWrapper
            ).result()

            val versionDidChange = version != lastVersion
            val actions = createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = toUpdate,
                version = newVersion,
                shouldStoreVersion = versionDidChange,
                syncType = this.type
            )
            finishSyncVersions(
                actions = actions,
                updateCount = toUpdate.size,
                objectS = objectS,
                libraryId = libraryId
            )
        } catch (e: Exception) {
            finishFailedSyncVersions(
                libraryId = libraryId,
                objectS = objectS,
                customResultError = CustomResult.GeneralError.CodeError(e),
                version = version
            )
        }
    }

    private suspend fun finishSyncVersions(
        actions: List<Action>,
        updateCount: Int,
        objectS: SyncObject,
        libraryId: LibraryIdentifier
    ) {
        //TODO update progress
        enqueue(actions = actions, index = 0)
    }

    private fun finishFailedSyncVersions(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        customResultError: CustomResult.GeneralError,
        version: Int
    ) {
        val syncError =
            syncError(customResultError = customResultError, data = SyncError.ErrorData.Companion.from(libraryId))
        when (syncError) {
            is SyncError.fatal2 -> {
                abort(error = syncError.error)
            }
            is SyncError.nonFatal2 -> {
                handleNonFatal(error = syncError.error, libraryId = libraryId, version = version)
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
                customResultError = CustomResult.GeneralError.CodeError(exception),
                data = SyncError.ErrorData.from(
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
        if (result !is CustomResult.GeneralSuccess) {
            val er = syncError(
                customResultError = result as CustomResult.GeneralError, data = SyncError.ErrorData.from(
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

    private fun createBatchedObjectActions(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        keys: List<String>,
        version: Int,
        shouldStoreVersion: Boolean,
        syncType: SyncType
    ): List<Action> {
        val batches = createBatchObjects(
            keys = keys,
            libraryId = libraryId,
            objectS = objectS,
            version = version
        )

        if (batches.isEmpty()) {
            if (shouldStoreVersion) {
                return listOf(Action.storeVersion(version, libraryId, objectS))
            } else {
                return listOf()
            }
        }


        var actions: MutableList<Action> = mutableListOf(Action.syncBatchesToDb(batches))
        if (shouldStoreVersion) {
            actions.add(Action.storeVersion(version, libraryId, objectS))
        }
        return actions
    }

    private fun createBatchObjects(
        keys: List<String>,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        version: Int
    ): List<DownloadBatch> {
        val maxBatchSize = DownloadBatch.maxCount
        var batchSize = 10
        var lowerBound = 0
        var batches: MutableList<DownloadBatch> = mutableListOf()

        while (lowerBound < keys.size) {
            val upperBound = min((keys.size - lowerBound), batchSize) + lowerBound
            val batchKeys = keys.subList(lowerBound, upperBound)

            batches.add(
                DownloadBatch(
                    libraryId = libraryId,
                    objectS = objectS,
                    keys = batchKeys,
                    version = version
                )
            )

            lowerBound += batchSize
            if (batchSize < maxBatchSize) {
                batchSize = min(batchSize * 2, maxBatchSize)
            }
        }

        return batches
    }


    private fun finish() {
        //TODO handle reportFinish - handle errors
        cleanup()
    }

    private fun abort(error: SyncError.Fatal) {
        Timber.i("Sync: aborted")
        Timber.i("Error: $error")
        itemResultsUseCase.postError(error)

        //TODO display error
        cleanup()
    }

    private fun handleNonFatal(
        error: SyncError.NonFatal,
        libraryId: LibraryIdentifier,
        version: Int?,
        additionalAction: (() -> Void)? = null
    ) {
        val appendAndContinue: () -> Unit = {
            this.nonFatalErrors.add(error)
            if (additionalAction != null) {
                additionalAction()
            }
            coroutineScope.launch {
                processNextAction()
            }
        }

        when (error) {
            is SyncError.NonFatal.versionMismatch -> {
                removeAllActions(libraryId = libraryId)
                appendAndContinue()
            }


            is SyncError.NonFatal.unchanged -> {
                if (version != null) {
                    //TODO handleUnchangedFailure
                } else {
                    if (additionalAction != null) {
                        additionalAction()
                    }
                    coroutineScope.launch {
                        processNextAction()
                    }
                }
            }
            is SyncError.NonFatal.quotaLimit -> {
                //TODO handleQuotaLimitFailure
            }

            else -> {
                appendAndContinue()
            }
        }
    }

    private fun removeAllActions(libraryId: LibraryIdentifier) {
        while (!this.queue.isEmpty()) {
            if (this.queue.firstOrNull()?.libraryId != libraryId) {
                break
            }
            this.queue.removeFirst()

        }
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
        customResultError: CustomResult.GeneralError,
        data: SyncError.ErrorData
    ): SyncError {
        return when (customResultError) {
            is CustomResult.GeneralError.CodeError -> {
                val error = customResultError.throwable
                Timber.e(error)
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
                return SyncError.nonFatal2(
                    SyncError.NonFatal.unknown(
                        error?.localizedMessage ?: ""
                    )
                )

            }
            is CustomResult.GeneralError.NetworkError -> {
                // TODO handle web dav errors

                //TODO handle reportMissing
                if (customResultError.isUnchanged()) {
                    return SyncError.nonFatal2(SyncError.NonFatal.unchanged)
                }


                return networkErrorRequiresAbort(
                    customResultError,
                    response = customResultError.stringResponse ?: "No Response",
                    data = data
                )
            }
        }
    }

    private fun networkErrorRequiresAbort(
        error: CustomResult.GeneralError.NetworkError,
        response: String,
        data: SyncError.ErrorData
    ): SyncError {
        val responseMessage: () -> String = {
            if (response == "No Response") {
                error.stringResponse ?: "No error to parse"
            } else {
                response
            }
        }

        val code = error.httpCode
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
