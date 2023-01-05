package org.zotero.android.sync

import io.realm.exceptions.RealmError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.Conflict
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.requests.PerformDeletionsDbRequest
import org.zotero.android.architecture.database.requests.UpdateVersionType
import org.zotero.android.backgrounduploader.BackgroundUploaderContext
import org.zotero.android.data.AccessPermissions
import org.zotero.android.data.mappers.CollectionResponseMapper
import org.zotero.android.data.mappers.ItemResponseMapper
import org.zotero.android.data.mappers.SearchResponseMapper
import org.zotero.android.data.mappers.UpdatesResponseMapper
import org.zotero.android.files.FileStore
import org.zotero.android.sync.SyncError.NonFatal
import org.zotero.android.sync.syncactions.DeleteGroupSyncAction
import org.zotero.android.sync.syncactions.LoadLibraryDataSyncAction
import org.zotero.android.sync.syncactions.LoadUploadDataSyncAction
import org.zotero.android.sync.syncactions.MarkChangesAsResolvedSyncAction
import org.zotero.android.sync.syncactions.MarkForResyncSyncAction
import org.zotero.android.sync.syncactions.MarkGroupAsLocalOnlySyncAction
import org.zotero.android.sync.syncactions.PerformDeletionsSyncAction
import org.zotero.android.sync.syncactions.StoreVersionSyncAction
import org.zotero.android.sync.syncactions.SubmitUpdateSyncAction
import org.zotero.android.sync.syncactions.SyncVersionsSyncAction
import org.zotero.android.sync.syncactions.UploadAttachmentSyncAction
import timber.log.Timber
import java.lang.Integer.min
import javax.inject.Inject
import javax.inject.Singleton


interface SyncAction<A : Any> {
    suspend fun result(): A
}

interface SyncActionWithError<A : Any> {
    suspend fun result(): CustomResult<A>
}

@Singleton
class SyncUseCase @Inject constructor(
    private val dispatcher: CoroutineDispatcher,
    private val syncRepository: SyncRepository,
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val syncApi: SyncApi,
    private val noAuthenticationApi: NoAuthenticationApi,
    private val fileStore: FileStore,
    private val backgroundUploaderContext: BackgroundUploaderContext,
    private val itemResponseMapper: ItemResponseMapper,
    private val collectionResponseMapper: CollectionResponseMapper,
    private val searchResponseMapper: SearchResponseMapper,
    private val schemaController: SchemaController,
    private val updatesResponseMapper: UpdatesResponseMapper,
    private val observable: SyncObservableEventStream,
    private val actionsCreator: ActionsCreator
) {

    private var coroutineScope = CoroutineScope(dispatcher)
    private var runningSyncJob: Job? = null

    private var userId: Long = 0L
    private var libraryType: LibrarySyncType = LibrarySyncType.all
    private var type: SyncType = SyncType.normal
    private var lastReturnedVersion: Int? = null
    private var conflictRetries: Int = 0
    private var conflictDelays: MutableList<Int> = mutableListOf()
    private var accessPermissions: AccessPermissions? = null

    private var queue = mutableListOf<Action>()
    private var processingAction: Action? = null
    private var previousType: SyncType? = null
    private var createActionOptions: CreateLibraryActionsOptions =
        CreateLibraryActionsOptions.automatic
    private var didEnqueueWriteActionsToZoteroBackend: Boolean = false
    private var enqueuedUploads: Int = 0
    private var uploadsFailedBeforeReachingZoteroBackend: Int = 0

    private var syncDelayIntervals: List<Double> = DelayIntervals.sync

    private var nonFatalErrors: MutableList<NonFatal> = mutableListOf()

    private val isSyncing: Boolean
        get() {
            return processingAction != null || queue.isNotEmpty()
        }

    private var batchProcessor: SyncBatchProcessor? = null

    fun init(userId: Long, conflictDelays: List<Int>, syncDelayIntervals: List<Double>) {
        this.conflictDelays = conflictDelays.toMutableList()
        this.syncDelayIntervals = syncDelayIntervals
        this.userId = userId
    }

    fun start(type: SyncType, libraries: LibrarySyncType) {
        runningSyncJob = coroutineScope.launch {
            with(this@SyncUseCase) {
                if (this.isSyncing) {
                    return@with
                }
                this.type = type
                this.libraryType = libraries
                queue.addAll(actionsCreator.createInitialActions(libraries = libraries, syncType = type))

                processNextAction()
            }

        }

    }

    private fun processNextAction() {
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

        runningSyncJob = coroutineScope.launch {
            process(action = action)
        }
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
            is Action.storeVersion -> {
                processStoreVersion(libraryId = action.libraryId, type = UpdateVersionType.objectS(action.syncObject), version = action.version)
            }
            is Action.storeDeletionVersion -> {
                processStoreVersion(libraryId = action.libraryId, type = UpdateVersionType.deletions, version = action.version)
            }
            is Action.performDeletions -> {
                performDeletions(libraryId = action.libraryId, collections = action.collections,
                    items = action.items, searches = action.searches, tags = action.tags, conflictMode = action.conflictMode)
            }
            is Action.markChangesAsResolved -> {
                markChangesAsResolved(action.libraryId)
            }
            is Action.markGroupAsLocalOnly -> {
                markGroupAsLocalOnly(action.groupId)
            }
            is Action.deleteGroup -> {
                deleteGroup(action.groupId)
            }
            is Action.createUploadActions -> {
                processCreateUploadActions(action.libraryId, hadOtherWriteActions = action.hadOtherWriteActions)
            }
            is Action.uploadAttachment -> {
                processUploadAttachment(action.upload)
            }
            is Action.submitWriteBatch -> {
                processSubmitUpdate(action.batch)
            }
            else -> {
                processNextAction()
            }
        }

    }

    private suspend fun processSubmitUpdate(batch: WriteBatch) {
        val actionResult = SubmitUpdateSyncAction(
            parameters = batch.parameters,
            changeUuids = batch.changeUuids,
            sinceVersion = batch.version,
            objectS = batch.objectS,
            libraryId = batch.libraryId,
            userId = this.userId,
            updateLibraryVersion = true,
            syncApi = this.syncApi,
            dbStorage = this.dbWrapper,
            fileStorage = this.fileStore,
            schemaController = this.schemaController,
            collectionResponseMapper = collectionResponseMapper,
            itemResponseMapper = itemResponseMapper,
        searchResponseMapper = searchResponseMapper,
        updatesResponseMapper = updatesResponseMapper, dispatcher = dispatcher).result()

        if (actionResult !is CustomResult.GeneralSuccess) {
            //TODO report uploaded progress
            finishSubmission(error = actionResult as CustomResult.GeneralError, newVersion = batch.version,
                keys = batch.parameters.mapNotNull { it["key"]?.toString() }, libraryId = batch.libraryId,
                objectS = batch.objectS
            )
        } else {
            //TODO report uploaded progress
            finishSubmission(error = actionResult.value!!.second, newVersion = actionResult.value!!.first,
                keys = batch.parameters.mapNotNull { it["key"]?.toString() }, libraryId = batch.libraryId,
                objectS = batch.objectS
            )
        }

    }

    private suspend fun processUploadAttachment(upload: AttachmentUpload) {
        val action = UploadAttachmentSyncAction(
            key = upload.key,
            file = upload.file,
            filename = upload.filename,
            md5 = upload.md5,
            mtime = upload.mtime,
            libraryId = upload.libraryId,
            userId = this.userId,
            oldMd5 = upload.oldMd5,
            syncApi = this.syncApi,
            dbWrapper = dbWrapper,
            fileStore = this.fileStore,
            schemaController = this.schemaController,
            noAuthenticationApi = noAuthenticationApi
        )

        val actionResult = action.result()
        if (actionResult !is CustomResult.GeneralSuccess) {
            //TODO report uploaded progress
            finishSubmission(error = actionResult as CustomResult.GeneralError, newVersion = null, keys = listOf(upload.key),
                libraryId = upload.libraryId, objectS = SyncObject.item, failedBeforeReachingApi = action.failedBeforeZoteroApiRequest)
        } else {
            //TODO report uploaded progress
            finishSubmission(error = null, newVersion = null, keys = listOf(upload.key),
                libraryId = upload.libraryId, objectS = SyncObject.item)
        }
    }

    private fun processBatchesSync(batches: List<DownloadBatch>) {
        val batch = batches.firstOrNull()
        if (batch == null) {
            processNextAction()
            return
        }

        val libraryId = batch.libraryId
        val objectS = batch.objectS

        this.batchProcessor =
            SyncBatchProcessor(batches = batches, userId = defaults.getUserId(), syncApi = syncApi,
                dbWrapper = this.dbWrapper, fileStore = this.fileStore,
                itemResponseMapper = itemResponseMapper,
                collectionResponseMapper = collectionResponseMapper,
                searchResponseMapper = searchResponseMapper,
                schemaController = schemaController,
                dispatcher = dispatcher,
                completion = { result ->
                    this.batchProcessor = null
                    finishBatchesSyncAction(libraryId, objectS = objectS, result = result)
                }
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
                val (failedKeys, parseErrors) = result.value!!
                this.nonFatalErrors.addAll(parseErrors.map({
                    syncError(customResultError = CustomResult.GeneralError.CodeError(it),
                        data = SyncError.ErrorData.from(libraryId = libraryId)).nonFatal2S
                        ?: NonFatal.unknown(it.localizedMessage ?:"") }))


                if (failedKeys.isEmpty()) {
                    processNextAction()
                } else {
                    coroutineScope.launch {
                        markForResync(keys = failedKeys, libraryId = libraryId, objectS = objectS)
                    }
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

    private suspend fun markForResync(
        keys: List<String>,
        libraryId: LibraryIdentifier,
        objectS: SyncObject
    ) {
        try {
            MarkForResyncSyncAction(keys = keys, objectS = objectS, libraryId = libraryId, dbStorage = this.dbWrapper).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            finishCompletableAction(errorData = Pair(e, SyncError.ErrorData.from(syncObject = objectS, keys = keys, libraryId = libraryId)))
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
                userId = defaults.getUserId(),
                syncDelayIntervals = this.syncDelayIntervals,
                checkRemote = checkRemote,
                syncApi = this.syncApi,
                dbWrapper = this.dbWrapper,
                dispatcher = this.dispatcher
            ).result()

            val versionDidChange = version != lastVersion
            val actions = actionsCreator.createBatchedObjectActions(
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

    private fun finishSyncVersions(
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
                fetchUpdates = (options != CreateLibraryActionsOptions.onlyDownloads),
                loadVersions = (this.type != SyncType.full),
                webDavEnabled = false,
                dbStorage = dbWrapper.realmDbStorage,
                defaults = defaults
            ).result()
            finishCreateLibraryActions(pair = result to options)
        } catch (e: Exception) {
            finishCreateLibraryActions(exception = e)
        }
    }

    private fun finishCreateLibraryActions(
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
        val (actions, queueIndex, writeCount) = actionsCreator.createLibraryActions(
            data = data,
            creationOptions = options,
            type = this.type
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
        val (toUpdate, toRemove) = result.value!!
        val actions =
            actionsCreator.createGroupActions(
                updateIds = toUpdate,
                deleteGroups = toRemove,
                syncType = type,
                libraryType = this.libraryType
            )
        finishSyncGroupVersions(actions = actions, updateCount = toUpdate.size)
    }

    private fun finishSyncGroupVersions(actions: List<Action>, updateCount: Int) {
        enqueue(actions = actions, index = 0)
    }

    private fun enqueue(
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
            runningSyncJob = coroutineScope.launch {
                delay(delayInSeconds * 1000L)
                processNextAction()
            }
        } else {
            processNextAction()
        }
    }


    private fun finish() {
        Timber.i("Sync: finished")
        if (!this.nonFatalErrors.isEmpty()) {
            Timber.i("Errors: ${this.nonFatalErrors}")
        }

        //TODO report finish progress

        reportFinish(this.nonFatalErrors)
        cleanup()
    }

    private fun abort(error: SyncError.Fatal) {
        Timber.i("Sync: aborted")
        Timber.i("Error: $error")
        //TODO report finish progress

        report(fatalError = error)
        cleanup()
    }

    private fun handleNonFatal(
        error: NonFatal,
        libraryId: LibraryIdentifier,
        version: Int?,
        additionalAction: (() -> Unit)? = null
    ) {
        val appendAndContinue: () -> Unit = {
            this.nonFatalErrors.add(error)
            if (additionalAction != null) {
                additionalAction()
            }
            processNextAction()
        }

        when (error) {
            is NonFatal.versionMismatch -> {
                removeAllActions(libraryId = libraryId)
                appendAndContinue()
            }


            is NonFatal.unchanged -> {
                if (version != null) {
                    //TODO handleUnchangedFailure
                } else {
                    if (additionalAction != null) {
                        additionalAction()
                    }
                    processNextAction()
                }
            }
            is NonFatal.quotaLimit -> {
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
        runningSyncJob?.cancel()
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

                val nonFatalError = error as? NonFatal
                if (nonFatalError != null) {
                    return SyncError.nonFatal2(error)
                }

                val syncActionError = error as? SyncActionError

                if (syncActionError != null) {
                    when (syncActionError) {
                        is SyncActionError.attachmentAlreadyUploaded, is SyncActionError.attachmentItemNotSubmitted ->
                            return SyncError.nonFatal2(
                                NonFatal.unknown(
                                    syncActionError.localizedMessage ?: ""
                                )
                            )
                        is SyncActionError.attachmentMissing ->
                            return SyncError.nonFatal2(
                                NonFatal.attachmentMissing(
                                    key = syncActionError.key,
                                    libraryId = syncActionError.libraryId,
                                    title = syncActionError.title
                                )
                            )
                        is SyncActionError.annotationNeededSplitting ->
                            return SyncError.nonFatal2(
                                NonFatal.annotationDidSplit(
                                    messageS = syncActionError.messageS,
                                    libraryId = syncActionError.libraryId
                                )
                            )
                        is SyncActionError.submitUpdateFailures ->
                            return SyncError.nonFatal2(NonFatal.unknown(syncActionError.messages))
                    }
                }

                // Check realm errors, every "core" error is bad. Can't create new Realm instance, can't continue with sync
                if (error is RealmError) {
                    Timber.e("received realm error - $error")
                    return SyncError.fatal2(SyncError.Fatal.dbError)
                }
                val schemaError = error as? SchemaError
                if (schemaError != null) {
                    return SyncError.nonFatal2(NonFatal.schema(error))
                }
                val parsingError = error as? Parsing.Error
                if (parsingError != null) {
                    return SyncError.nonFatal2(NonFatal.parsing(error))
                }
                Timber.e("received unknown error - $error")
                return SyncError.nonFatal2(
                    NonFatal.unknown(
                        error?.localizedMessage ?: ""
                    )
                )

            }
            is CustomResult.GeneralError.NetworkError -> {
                // TODO handle web dav errors

                //TODO handle reportMissing
                if (customResultError.isUnchanged()) {
                    return SyncError.nonFatal2(NonFatal.unchanged)
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
                return SyncError.nonFatal2(NonFatal.unchanged)
            413 ->
                return SyncError.nonFatal2(NonFatal.quotaLimit(data.libraryId))
            507 ->
                return SyncError.nonFatal2(NonFatal.insufficientSpace)
            503 ->
                return SyncError.fatal2(SyncError.Fatal.serviceUnavailable)
            403 ->
                return SyncError.fatal2(SyncError.Fatal.forbidden)
            else -> {
                if (code >= 400 && code <= 499) {
                    return SyncError.fatal2(
                        SyncError.Fatal.apiError(
                            response = responseMessage(),
                            data = data
                        )
                    )
                } else {
                    return SyncError.nonFatal2(
                        NonFatal.apiError(
                            response = responseMessage(),
                            data = data
                        )
                    )
                }
            }
        }
    }

    private suspend fun processStoreVersion(libraryId: LibraryIdentifier, type: UpdateVersionType, version: Int) {
        try {
            StoreVersionSyncAction(version =  version, type =  type, libraryId = libraryId, dbWrapper = this.dbWrapper).result()
            finishCompletableAction(null)
        }catch (e: Throwable) {
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private fun finishCompletableAction(errorData: Pair<Throwable, SyncError.ErrorData>?) {
        if (errorData == null) {
            processNextAction()
            return
        }
        val (error, data) = errorData

        val syncError = syncError(CustomResult.GeneralError.CodeError(error), data)
        when (syncError) {
            is SyncError.fatal2 -> {
                abort(syncError.error)
            }
            is SyncError.nonFatal2 -> {
                handleNonFatal(error = syncError.error, libraryId = data.libraryId, version = null)
            }
        }
    }

    private suspend fun performDeletions(libraryId: LibraryIdentifier, collections: List<String>,
                                         items: List<String>, searches: List<String>, tags: List<String>,
                                         conflictMode: PerformDeletionsDbRequest.ConflictResolutionMode) {
        try {
            val conflicts = PerformDeletionsSyncAction(libraryId = libraryId, collections = collections,
                items = items, searches = searches, tags = tags,
                conflictMode = conflictMode, dbWrapper = dbWrapper).result()
            finishDeletionsSync(result = conflicts, libraryId = libraryId)
        }catch(e: Throwable) {
            finishDeletionsSync(e = e, libraryId = libraryId)
        }
    }

    private fun finishDeletionsSync(
        e: Throwable? = null,
        result: List<Pair<String, String>>? = null,
        libraryId: LibraryIdentifier, version: Int? = null
    ) {
        if (e != null) {
            val syncError = syncError(
                CustomResult.GeneralError.CodeError(e),
                data = SyncError.ErrorData.from(libraryId = libraryId)
            )
            when (syncError) {
                is SyncError.fatal2 ->
                abort(error = syncError.error)
                is SyncError.nonFatal2 ->
                handleNonFatal(error = syncError.error, libraryId = libraryId, version = version)
            }
            return
        }

        val conflicts = result!!
        if (!conflicts.isEmpty()) {
            resolve(conflict = Conflict.removedItemsHaveLocalChanges(keys = conflicts, libraryId = libraryId))
        } else {
            processNextAction()
        }
    }

    private fun resolve(conflict: Conflict) {
        //TODO implement conflict resolution
        processNextAction()
    }

    private suspend fun markChangesAsResolved(libraryId: LibraryIdentifier) {
        try {
            MarkChangesAsResolvedSyncAction(libraryId = libraryId, dbWrapper = dbWrapper).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private suspend fun markGroupAsLocalOnly(groupId: Int) {
        try {
            MarkGroupAsLocalOnlySyncAction(groupId = groupId, dbWrapper = dbWrapper).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = LibraryIdentifier.group(groupId)))
        }
    }

    private suspend fun deleteGroup(groupId: Int) {
        try {
            DeleteGroupSyncAction(groupId =  groupId, dbWrapper = dbWrapper).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = LibraryIdentifier.group(groupId)))
        }

    }

    private suspend fun processCreateUploadActions(libraryId: LibraryIdentifier, hadOtherWriteActions: Boolean) {
        try {
            val uploads = LoadUploadDataSyncAction(libraryId = libraryId, backgroundUploaderContext = backgroundUploaderContext, dbWrapper = dbWrapper,
                fileStore = fileStore).result()
            process(uploads = uploads, hadOtherWriteActions = hadOtherWriteActions, libraryId = libraryId)
        } catch (e: Exception) {
            enqueuedUploads = 0
            uploadsFailedBeforeReachingZoteroBackend = 0

            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private fun process(uploads: List<AttachmentUpload>, hadOtherWriteActions: Boolean, libraryId: LibraryIdentifier) {
        if (uploads.isEmpty()) {
            if (hadOtherWriteActions) {
                processNextAction()
                return
            }

            this.queue.add(index = 0, Action.createLibraryActions(LibrarySyncType.specific(listOf(libraryId)), CreateLibraryActionsOptions.onlyDownloads))
            processNextAction()
            return
        }

        //TODO report progress
        enqueuedUploads = uploads.size
        uploadsFailedBeforeReachingZoteroBackend = 0
        enqueue(actions = uploads.map { Action.uploadAttachment(it) }, index = 0)
    }

    private fun finishSubmission(error: CustomResult.GeneralError?, newVersion: Int?, keys: List<String>, libraryId: LibraryIdentifier,
                                         objectS: SyncObject, failedBeforeReachingApi: Boolean = false, ignoreWebDav: Boolean = false) {
        val nextAction = {
            if (newVersion != null) {
                updateVersionInNextWriteBatch(newVersion)
            }
            processNextAction()
        }

         if (error == null) {
            nextAction()
            return
        }

        val syncActionError = (error as? CustomResult.GeneralError.CodeError)?.throwable
                as? SyncActionError
        if (syncActionError != null) {
            when(syncActionError) {
                is SyncActionError.attachmentAlreadyUploaded -> {
                    nextAction()
                    return
                }
            }
        }

        if (handleUpdatePreconditionFailureIfNeeded(error, libraryId = libraryId)) {
            return
        }

       //TODO handle webdav error

        val er = syncError(
            customResultError = error, data = SyncError.ErrorData.from(
                syncObject = objectS, keys = keys, libraryId = libraryId)
            )
        when (er) {
            is SyncError.fatal2 ->
                abort(error = er.error)
            is SyncError.nonFatal2 -> {
                handleNonFatal(error = er.error, libraryId = libraryId, version = newVersion, additionalAction = {
                    if (newVersion != null) {
                        updateVersionInNextWriteBatch(newVersion)
                    }
                    if (failedBeforeReachingApi) {
                        handleAllUploadsFailedBeforeReachingZoteroBackend(libraryId)
                    }
                })
            }
        }
    }

    private fun updateVersionInNextWriteBatch(version: Int) {
        val action = this.queue.firstOrNull()
        if (action == null) {
            return
        }
        when (action) {
            is Action.submitWriteBatch -> {
                val updatedBatch = action.batch.copy(version=version)
                queue[0]=Action.submitWriteBatch(updatedBatch)
            }
            is Action.submitDeleteBatch -> {
                val updatedBatch = action.batch.copy(version=version)
                this.queue[0]=Action.submitDeleteBatch(updatedBatch)
            }
        }
    }

    private fun handleUpdatePreconditionFailureIfNeeded(error: CustomResult.GeneralError, libraryId: LibraryIdentifier): Boolean {
        val preconditionError = error.preconditionError
        if (preconditionError == null) {
            return false
        }

        if (this.createActionOptions == CreateLibraryActionsOptions.onlyWrites) {
            this.abort(error= SyncError.Fatal.preconditionErrorCantBeResolved)
            return true
        }

        when(preconditionError) {
            is PreconditionErrorType.objectConflict -> {
                if (this.conflictRetries >= this.conflictDelays.size) {
                    abort(SyncError.Fatal.uploadObjectConflict)
                    return true
                }

                Timber.e("SyncController: object conflict - trying full sync")

                val delay = this.conflictDelays[min(this.conflictRetries, (this.conflictDelays.size - 1))]
                val actions = actionsCreator.createInitialActions(this.libraryType, syncType = SyncType.full)

                this.type = SyncType.full
                this.conflictRetries = this.conflictDelays.size

                this.queue.clear()
                enqueue(actions = actions, index = 0, delayInSeconds = delay)
            }


            is PreconditionErrorType.libraryConflict -> {
                if (this.conflictRetries >= this.conflictDelays.size) {
                    abort(SyncError.Fatal.cantResolveConflict)
                    return true
                }

                Timber.e("SyncController: library conflict - re-downloading library objects and trying writes again")

                val delay = this.conflictDelays[min(this.conflictRetries, (this.conflictDelays.size - 1))]
                val actions = listOf(Action.createLibraryActions(LibrarySyncType.specific(listOf(libraryId)),
                    CreateLibraryActionsOptions.onlyDownloads
                ),
                Action.createLibraryActions(LibrarySyncType.specific(listOf(libraryId)),
                CreateLibraryActionsOptions.onlyWrites
                ))

                this.conflictRetries += 1

                removeAllActions(libraryId)
                enqueue(actions = actions, index =  0, delayInSeconds = delay)
            }

        }

        return true
    }

    private fun handleAllUploadsFailedBeforeReachingZoteroBackend(libraryId: LibraryIdentifier) {
        if (didEnqueueWriteActionsToZoteroBackend || !(this.enqueuedUploads > 0)) {
            return
        }
        this.uploadsFailedBeforeReachingZoteroBackend += 1

        if (!(this.enqueuedUploads == this.uploadsFailedBeforeReachingZoteroBackend) || !(this.queue.firstOrNull()?.libraryId != libraryId) ) {
            return
        }

        this.didEnqueueWriteActionsToZoteroBackend = false
        this.enqueuedUploads = 0
        this.uploadsFailedBeforeReachingZoteroBackend = 0
        this.queue.add(element=Action.createLibraryActions(LibrarySyncType.specific(listOf(libraryId)),
            CreateLibraryActionsOptions.onlyDownloads
        ), index = 0)
    }

    fun cancel() {
        if (!this.isSyncing) {
            return
        }
        Timber.i("Sync: cancelled")
        cleanup()
        report(fatalError = SyncError.Fatal.cancelled)
    }

    private fun report(fatalError: SyncError.Fatal) {
        //TODO report abort progress
        this.previousType = null

        when (fatalError) {
            SyncError.Fatal.uploadObjectConflict ->
                this.observable.emitAsync(SchedulerAction(SyncType.full, LibrarySyncType.all))
            else ->
                this.observable.emitAsync(null)
        }

    }

    private fun reportFinish(errors: List<NonFatal>) {
        // Find libraries which reported version mismatch.
        var retryLibraries = mutableListOf<LibraryIdentifier>()
        var reportErrors = mutableListOf<NonFatal>()

        for (error in errors) {
            when (error) {
                is NonFatal.versionMismatch -> {
                    if (!retryLibraries.contains(error.libraryId)) {
                        retryLibraries.add(error.libraryId)
                    }
                }
                is NonFatal.annotationDidSplit -> {
                    if (!retryLibraries.contains(error.libraryId)) {
                        retryLibraries.add(error.libraryId)
                    }
                }
                //TODO handle webDavVerification && webDavDownload
                is NonFatal.unknown, is NonFatal.schema, is NonFatal.parsing, is NonFatal.apiError,
                is NonFatal.unchanged, is NonFatal.quotaLimit, is NonFatal.attachmentMissing,
                is NonFatal.insufficientSpace, is NonFatal.webDavDeletion, is NonFatal.webDavDeletionFailed ->
                reportErrors.add(error)
            }
        }

        if (retryLibraries.isEmpty()) {
            //TODO report progress finish
            this.previousType = null
            this.observable.emitAsync(null)
            return
        }

        if (this.previousType == null) {
            //TODO report progress finish
            this.previousType = this.type
            this.observable.emitAsync(SchedulerAction(this.type, LibrarySyncType.specific(retryLibraries)))
        } else {
            //TODO report progress finish
            this.previousType = null
            this.observable.emitAsync(null)
        }
    }


}
