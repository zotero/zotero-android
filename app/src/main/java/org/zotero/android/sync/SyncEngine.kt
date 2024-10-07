package org.zotero.android.sync

import com.google.gson.Gson
import io.realm.exceptions.RealmError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.mappers.CollectionResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.SearchResponseMapper
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgressHandler
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.MarkObjectsAsChangedByUser
import org.zotero.android.database.requests.PerformDeletionsDbRequest
import org.zotero.android.database.requests.ReadGroupDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.files.FileStore
import org.zotero.android.sync.SyncError.NonFatal
import org.zotero.android.sync.conflictresolution.Conflict
import org.zotero.android.sync.conflictresolution.ConflictEventStream
import org.zotero.android.sync.conflictresolution.ConflictResolution
import org.zotero.android.sync.syncactions.DeleteGroupSyncAction
import org.zotero.android.sync.syncactions.DeleteWebDavFilesSyncAction
import org.zotero.android.sync.syncactions.FetchAndStoreGroupSyncAction
import org.zotero.android.sync.syncactions.LoadDeletionsSyncAction
import org.zotero.android.sync.syncactions.LoadLibraryDataSyncAction
import org.zotero.android.sync.syncactions.LoadPermissionsSyncAction
import org.zotero.android.sync.syncactions.LoadUploadDataSyncAction
import org.zotero.android.sync.syncactions.MarkChangesAsResolvedSyncAction
import org.zotero.android.sync.syncactions.MarkForResyncSyncAction
import org.zotero.android.sync.syncactions.MarkGroupAsLocalOnlySyncAction
import org.zotero.android.sync.syncactions.MarkGroupForResyncSyncAction
import org.zotero.android.sync.syncactions.PerformDeletionsSyncAction
import org.zotero.android.sync.syncactions.RestoreDeletionsSyncAction
import org.zotero.android.sync.syncactions.RevertLibraryFilesSyncAction
import org.zotero.android.sync.syncactions.RevertLibraryUpdatesSyncAction
import org.zotero.android.sync.syncactions.StoreVersionSyncAction
import org.zotero.android.sync.syncactions.SubmitDeletionSyncAction
import org.zotero.android.sync.syncactions.SubmitUpdateSyncAction
import org.zotero.android.sync.syncactions.SyncSettingsSyncAction
import org.zotero.android.sync.syncactions.SyncVersionsSyncAction
import org.zotero.android.sync.syncactions.UploadAttachmentSyncAction
import org.zotero.android.sync.syncactions.UploadFixSyncAction
import org.zotero.android.sync.syncactions.data.AccessPermissions
import org.zotero.android.sync.syncactions.data.ZoteroApiError
import org.zotero.android.webdav.WebDavSessionStorage
import org.zotero.android.webdav.data.WebDavError
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val defaults: Defaults,
    private val dbWrapperMain: DbWrapperMain,
    private val zoteroApi: ZoteroApi,
    private val fileStore: FileStore,
    private val itemResponseMapper: ItemResponseMapper,
    private val collectionResponseMapper: CollectionResponseMapper,
    private val searchResponseMapper: SearchResponseMapper,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
    private val observable: SyncObservableEventStream,
    private val actionsCreator: ActionsCreator,
    private val gson: Gson,
    private val conflictEventStream: ConflictEventStream,
    private val progressHandler: SyncProgressHandler,
    private val sessionStorage: WebDavSessionStorage,
) {
    private var userId: Long = 0L
    private var libraryType: Libraries = Libraries.all
    private var type: SyncKind = SyncKind.normal
    private var lastReturnedVersion: Int? = null
    private var retryAttempt: Int = 0
    private var maxRetryCount: Int = 0
    private var accessPermissions: AccessPermissions? = null

    private var queue = mutableListOf<Action>()
    private var processingAction: Action? = null
//    private var createActionOptions: CreateLibraryActionsOptions =
//        CreateLibraryActionsOptions.automatic
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
    private lateinit var syncSchedulerCoroutineScope: CoroutineScope


    fun init(userId: Long, syncDelayIntervals: List<Double>, maxRetryCount: Int) {
        this.syncDelayIntervals = syncDelayIntervals
        this.userId = userId
        this.maxRetryCount = maxRetryCount
    }

    suspend fun start(type: SyncKind, libraries: Libraries, retryAttempt: Int, syncSchedulerCoroutineScope: CoroutineScope) {
        Timber.i("SyncEngine: start with syncKind = $type")
        with(this@SyncUseCase) {
            if (this.isSyncing) {
                Timber.i("SyncEngine: already syncing. Not proceeding with this sync request")
                return@with
            }
            this.type = type
            this.syncSchedulerCoroutineScope = syncSchedulerCoroutineScope
            this.libraryType = libraries
            this.retryAttempt = retryAttempt
            this.progressHandler.reportNewSync()
            queue.addAll(
                actionsCreator.createInitialActions(
                    libraries = libraries,
                    syncType = type
                )
            )

            processNextAction()
        }
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

        process(action = action)
    }

    private suspend fun process(action: Action) {
        when (action) {
            is Action.loadKeyPermissions -> {
                processKeyCheckAction()
            }
            is Action.createLibraryActions -> {
                processCreateLibraryActions(
                    libraries = action.librarySyncType,
                    options = action.createLibraryActionsOptions
                )
            }
            is Action.submitDeleteBatch -> {
                processSubmitDeletion(action.batch)
            }

            is Action.syncGroupVersions -> {
                this.progressHandler.reportGroupsSync()
                processSyncGroupVersions()
            }
            is Action.syncVersions -> {
                this.progressHandler.reportObjectSync(action.objectS, action.libraryId)
                processSyncVersions(
                    libraryId = action.libraryId,
                    objectS = action.objectS,
                    version = action.version,
                    checkRemote = action.checkRemote
                )
            }

            is Action.resolveDeletedGroup -> {
                resolve(
                    Conflict.groupRemoved(
                        groupId = action.groupId,
                        name = action.name
                    )
                )
            }
            is Action.syncGroupToDb -> {
                processGroupSync(action.groupId)
            }

            is Action.resolveGroupMetadataWritePermission -> {
                resolve(
                    Conflict.groupMetadataWriteDenied(
                        groupId = action.groupId,
                        name = action.name
                    )
                )
            }
            is Action.resolveGroupFileWritePermission -> {
                resolve(conflict = Conflict.groupFileWriteDenied(groupId = action.groupId, name = action.name))
            }

            is Action.fixUpload -> {
                processUploadFix(key = action.key, libraryId = action.libraryId)
            }
            is Action.removeActions -> {
                removeAllActions(action.libraryId)
            }
            is Action.revertLibraryFilesToOriginal -> {
                revertGroupFiles(action.libraryId)
            }

            is Action.syncDeletions -> {
                this.progressHandler.reportDeletions(action.libraryId)
                loadRemoteDeletions(libraryId = action.libraryId, sinceVersion = action.version)
            }

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
                processCreateUploadActions(action.libraryId, hadOtherWriteActions = action.hadOtherWriteActions, canWriteFiles = action.canEditFiles)
            }
            is Action.uploadAttachment -> {
                processUploadAttachment(action.upload)
            }
            is Action.submitWriteBatch -> {
                processSubmitUpdate(action.batch)
            }
            is Action.revertLibraryToOriginal -> {
                revertGroupData(action.libraryId)
            }
            is Action.restoreDeletions -> {
                restoreDeletions(libraryId = action.libraryId, collections = action.collections, items = action.items)
            }
            is Action.syncSettings -> {
                this.progressHandler.reportLibrarySync(action.libraryId)
                processSettingsSync(libraryId = action.libraryId, version = action.version)
            }
            is Action.performWebDavDeletions -> {
                performWebDavDeletions(libraryId = action.libraryId)
            }
            else -> {
                processNextAction()
            }
        }
    }

    private suspend fun processKeyCheckAction() {
        val result = LoadPermissionsSyncAction().result()
        if (result is CustomResult.GeneralSuccess) {
            val response = result.value!!
            val permissions = AccessPermissions(
                user = response.user,
                groupDefault = response.defaultGroup,
                groups = response.groups
            )
            val group = permissions.groupDefault
            if (group != null && (!group.library || !group.write)) {
                processKeyCheckActionFailure(CustomResult.GeneralError.CodeError(SyncError.Fatal.missingGroupPermissions))
                return
            }
            val username = response.username
            val displayName = response.displayName
            defaults.setUsername(username)
            defaults.setDisplayName(displayName)
            this.accessPermissions = permissions
            processNextAction()
        } else {
            val customResultError = result as CustomResult.GeneralError
            processKeyCheckActionFailure(customResultError)
        }
    }

    private fun processKeyCheckActionFailure(customResultError: CustomResult.GeneralError) {
        val er = syncError(
            customResultError = customResultError,
            data = SyncError.ErrorData.from(
                libraryId = LibraryIdentifier.custom(
                    RCustomLibraryType.myLibrary
                )
            )
        ).fatal2S ?: SyncError.Fatal.permissionLoadingFailed
        abort(er)
    }

    private suspend fun processSettingsSync(libraryId: LibraryIdentifier, version: Int) {
        val result = SyncSettingsSyncAction(
            currentVersion = this.lastReturnedVersion,
            sinceVersion = version,
            libraryId = libraryId,
            userId = this.userId,
        ).result()
        finishSettingsSync(result = result, libraryId = libraryId, version = version)
    }

    private suspend fun finishSettingsSync(
        result: CustomResult<Pair<Boolean, Int>>,
        libraryId: LibraryIdentifier,
        version: Int
    ) {
        when (result) {
            is CustomResult.GeneralSuccess -> {
                val hasNewSettings = result.value!!.first
                val version = result.value!!.second
                Timber.i("Sync: store version - $version")
                this.lastReturnedVersion = version
                if (hasNewSettings) {
                    enqueue(
                        actions = listOf(
                            Action.storeVersion(
                                version,
                                libraryId,
                                SyncObject.settings
                            )
                        ), index = 0
                    )
                } else {
                    processNextAction()
                }
            }

            is CustomResult.GeneralError -> {
                val syncError = syncError(
                    customResultError = result,
                    data = SyncError.ErrorData.from(libraryId)
                )
                when (syncError) {
                    is SyncError.fatal2 -> {
                        abort(syncError.error)
                    }

                    is SyncError.nonFatal2 -> {
                        handleNonFatal(
                            error = syncError.error,
                            libraryId = libraryId,
                            version = version
                        )
                    }
                }
            }
        }
    }

    private suspend fun restoreDeletions(
        libraryId: LibraryIdentifier,
        collections: List<String>,
        items: List<String>
    ) {
        val result = RestoreDeletionsSyncAction(
            libraryId = libraryId,
            collections = collections,
            items = items
        ).result()
        if (result is CustomResult.GeneralError.CodeError) {
            Timber.e(result.throwable)
            finishCompletableAction(
                errorData = Pair(
                    result.throwable,
                    SyncError.ErrorData(itemKeys = items, libraryId = libraryId)
                )
            )
            return
        }
        finishCompletableAction(errorData = null)
    }

    private suspend fun revertGroupData(libraryId: LibraryIdentifier) {
        val result = RevertLibraryUpdatesSyncAction(
            libraryId = libraryId,
        ).result()
        if (result is CustomResult.GeneralError.CodeError) {
            Timber.e(result.throwable)
            finishCompletableAction(errorData = Pair(result.throwable, SyncError.ErrorData.from(libraryId = libraryId)))
            return
        }
        finishCompletableAction(errorData = null)
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
        ).result()

        if (actionResult !is CustomResult.GeneralSuccess) {
            this.progressHandler.reportWriteBatchSynced(size = batch.parameters.size)
            finishSubmission(error = actionResult as CustomResult.GeneralError, newVersion = batch.version,
                keys = batch.parameters.mapNotNull { it["key"]?.toString() }, libraryId = batch.libraryId,
                objectS = batch.objectS
            )
        } else {
            this.progressHandler.reportWriteBatchSynced(size = batch.parameters.size)
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
        )

        val actionResult = action.result()
        if (actionResult !is CustomResult.GeneralSuccess) {
            this.progressHandler.reportUploaded()
            finishSubmission(error = actionResult as CustomResult.GeneralError, newVersion = null, keys = listOf(upload.key),
                libraryId = upload.libraryId, objectS = SyncObject.item, failedBeforeReachingApi = action.failedBeforeZoteroApiRequest)
        } else {
            this.progressHandler.reportUploaded()
            finishSubmission(error = null, newVersion = null, keys = listOf(upload.key),
                libraryId = upload.libraryId, objectS = SyncObject.item)
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
            SyncBatchProcessor(
                batches = batches,
                userId = defaults.getUserId(),
                zoteroApi = zoteroApi,
                dbWrapperMain = this.dbWrapperMain,
                fileStore = this.fileStore,
                itemResponseMapper = itemResponseMapper,
                collectionResponseMapper = collectionResponseMapper,
                searchResponseMapper = searchResponseMapper,
                schemaController = schemaController,
                dateParser = this.dateParser,
                gson = this.gson,
                progress = { processed ->
                    syncSchedulerCoroutineScope.launch {
                        this@SyncUseCase.progressHandler.reportDownloadBatchSynced(
                            size = processed,
                            objectS = objectS,
                            libraryId = libraryId
                        )
                    }

                },
                completion = { result ->
                    syncSchedulerCoroutineScope.launch {
                        this@SyncUseCase.batchProcessor?.cancelAllOperations()
                        this@SyncUseCase.batchProcessor = null
                        val keys = batches.flatMap { it.keys }
                        finishBatchesSyncAction(
                            libraryId = libraryId,
                            objectS = objectS,
                            result = result,
                            keys = keys
                        )
                    }

                }
            )
        this.batchProcessor?.start()
    }

    private suspend fun finishBatchesSyncAction(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        result: CustomResult<SyncBatchResponse>,
        keys: List<String>
    ) {
        when (result) {
            is CustomResult.GeneralSuccess -> {
                val (failedKeys, parseErrors) = result.value!!

                val nonFatalErrors = parseErrors.map {
                    syncError(
                        customResultError = CustomResult.GeneralError.CodeError(it),
                        data = SyncError.ErrorData.from(
                            libraryId = libraryId,
                            syncObject = objectS,
                            keys = failedKeys
                        )
                    ).nonFatal2S
                        ?: NonFatal.unknown(
                            messageS = it.localizedMessage ?: "",
                            data = SyncError.ErrorData.from(
                                syncObject = objectS,
                                keys = failedKeys,
                                libraryId = libraryId
                            )
                        )
                }
                this.nonFatalErrors.addAll(nonFatalErrors)

                if (failedKeys.isEmpty()) {
                    processNextAction()
                } else {
                    markForResync(keys = failedKeys, libraryId = libraryId, objectS = objectS)
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
                        handleNonFatal(
                            error = syncError.error,
                            libraryId = libraryId,
                            version = null
                        ) {
                            markForResync(keys = keys, libraryId = libraryId, objectS = objectS)
                            return@handleNonFatal false
                        }
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
            MarkForResyncSyncAction(
                keys = keys,
                objectS = objectS,
                libraryId = libraryId
            ).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            finishCompletableAction(
                errorData = Pair(
                    e, SyncError.ErrorData.from(
                        syncObject = objectS,
                        keys = keys,
                        libraryId = libraryId
                    )
                )
            )
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
            ).result()

            val versionDidChange = version != lastVersion
            val actions = actionsCreator.createBatchedObjectActions(
                libraryId = libraryId,
                objectS = objectS,
                keys = toUpdate,
                version = newVersion,
                shouldStoreVersion = versionDidChange,
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
        this.progressHandler.reportDownloadCount(
            objectS = objectS,
            count = updateCount,
            libraryId = libraryId
        )
        enqueue(actions = actions, index = 0)
    }

    private suspend fun finishFailedSyncVersions(
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        customResultError: CustomResult.GeneralError,
        version: Int
    ) {
        val syncError =
            syncError(customResultError = customResultError, data = SyncError.ErrorData.from(libraryId))
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
        libraries: Libraries,
        options: CreateLibraryActionsOptions
    ) {
        try {
            val result = LoadLibraryDataSyncAction(
                type = libraries,
                fetchUpdates = (options != CreateLibraryActionsOptions.onlyDownloads),
                loadVersions = (this.type != SyncKind.full),
                webDavEnabled = sessionStorage.isEnabled,
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
        if (options == CreateLibraryActionsOptions.automatic || this.type == SyncKind.full) {
            val nameDictionary = mutableMapOf<LibraryIdentifier, String>()
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

        val names = libraryNames
        if (names != null) {
            progressHandler.set(libraryNames = names)
        }
        if (writeCount > 0) {
            progressHandler.reportWrite(writeCount)
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

    private suspend fun finishSyncGroupVersions(actions: List<Action>, updateCount: Int) {
        this.progressHandler.reportGroupCount(count = updateCount)
        enqueue(actions = actions, index = 0)
    }

    private suspend fun enqueue(
        actions: List<Action>,
        index: Int? = null,
    ) {
        if (actions.isNotEmpty()) {
            if (index != null) {
                queue.addAll(index, actions)
            } else {
                queue.addAll(actions)
            }
        }
        processNextAction()
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

    private suspend fun handleNonFatal(
        error: NonFatal,
        libraryId: LibraryIdentifier,
        version: Int?,
        additionalAction:  (suspend () -> Boolean)? = null
    ) {
        val appendAndContinue: suspend () -> Unit = {
            this.nonFatalErrors.add(error)
            val extraAction = additionalAction?.invoke() ?: true
            if (extraAction) {
                processNextAction()
            }
        }

        when (error) {
            is NonFatal.versionMismatch, is NonFatal.preconditionFailed -> {
                removeAllActions(libraryId = libraryId)
                appendAndContinue()
            }
            is NonFatal.unchanged -> {
                if (version != null) {
                    handleUnchangedFailure(
                        lastVersion = version,
                        libraryId = libraryId,
                        additionalAction = additionalAction
                    )
                } else {
                    if (additionalAction != null) {
                        additionalAction()
                    }
                    processNextAction()
                }
            }
            is NonFatal.quotaLimit -> {
                handleQuotaLimit(
                    libraryId = libraryId,
                    error = error,
                    additionalAction = additionalAction
                )
            }

            else -> {
                appendAndContinue()
            }
        }
    }

    private suspend fun handleQuotaLimit(
        libraryId: LibraryIdentifier,
        error: NonFatal.quotaLimit,
        additionalAction: (suspend () -> Boolean)?
    ) {
        Timber.i("Sync: received quota limit for $libraryId")
        this.queue.removeAll {action ->
            when (action) {
                is Action.uploadAttachment -> {
                    action.upload.libraryId == libraryId
                }
                else -> {
                    false
                }
            }
        }
        this.queue.add(index = 0, Action.createLibraryActions(Libraries.specific(listOf(libraryId)), CreateLibraryActionsOptions.onlyDownloads))

        if (!this.nonFatalErrors.contains(error)) {
            this.nonFatalErrors.add(error)
        }

        val shouldProcessNext = additionalAction?.invoke() ?: true
        if (shouldProcessNext) {
            processNextAction()
        }
    }

    private suspend fun handleUnchangedFailure(
        lastVersion: Int,
        libraryId: LibraryIdentifier,
        additionalAction: (suspend () -> Boolean)?
    ) {
        Timber.i("Sync: received unchanged error, store version: $lastVersion")
        this.lastReturnedVersion = lastVersion

        if(this.type == SyncKind.full) {
            val shouldProcessNext = additionalAction?.invoke() ?: true
            if (shouldProcessNext) {
                processNextAction()
            }
            return
        }

        val toDelete = mutableListOf<Int>()
        for ((index, action) in this.queue.withIndex()) {
            if (action.libraryId != libraryId) { break }
            when (action) {
                is Action.syncVersions -> {
                    this.queue[index] = Action.syncVersions(
                        libraryId = action.libraryId,
                        objectS = action.objectS,
                        version = action.version,
                        checkRemote = action.version < lastVersion
                    )
                }
                is Action.syncSettings -> {
                    if (lastVersion == action.version) {
                        toDelete.add(index)
                    }
                }
                is Action.syncDeletions -> {
                    if (lastVersion == action.version) {
                        toDelete.add(index)
                    }
                }
                is Action.storeDeletionVersion -> {
                    if (lastVersion == action.version) {
                        toDelete.add(index)
                    }
                }
                else -> {}
            }
        }

        toDelete.reversed().forEach { this.queue.removeAt(it) }
        val extraAction = additionalAction?.invoke() ?: true
        if (!extraAction) { return }
        processNextAction()
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
//        runningSyncJob?.cancel()
        processingAction = null
        queue = mutableListOf()
        nonFatalErrors = mutableListOf()
        type = SyncKind.normal
        lastReturnedVersion = null
        accessPermissions = null
        this.batchProcessor?.cancelAllOperations()
        this.batchProcessor = null
        libraryType = Libraries.all
        didEnqueueWriteActionsToZoteroBackend = false
        enqueuedUploads = 0
        uploadsFailedBeforeReachingZoteroBackend = 0
        retryAttempt = 0
    }

    private fun syncError(
        customResultError: CustomResult.GeneralError,
        data: SyncError.ErrorData
    ): SyncError {
        when (customResultError) {
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
                                    libraryId = syncActionError.libraryId,
                                    keys = syncActionError.keys
                                )
                            )
                        is SyncActionError.submitUpdateFailures ->
                            return SyncError.nonFatal2(NonFatal.unknown(messageS = syncActionError.messageS, data = data))
                        is SyncActionError.authorizationFailed -> {
                            return SyncError.nonFatal2(NonFatal.unknown(messageS = syncActionError.response, data = data))
                        }
                        is SyncActionError.attachmentAlreadyUploaded, is SyncActionError.attachmentItemNotSubmitted -> {
                            return SyncError.nonFatal2(NonFatal.unknown(messageS = error.localizedMessage!!, data = data))
                        }
                        is SyncActionError.objectPreconditionError -> {
                            return SyncError.fatal2(SyncError.Fatal.uploadObjectConflict(data))
                        }
                    }
                }

                if (error is WebDavError.Verification) {
                    return SyncError.nonFatal2(NonFatal.webDavVerification(error))
                }

                if (error is WebDavError.Download) {
                    return SyncError.nonFatal2(NonFatal.webDavDownload(error))
                }

                if (error is WebDavError.Upload) {
                    return SyncError.nonFatal2(NonFatal.webDavUpload(error))
                }

                if (error is ZoteroApiError) {
                    when (error) {
                        ZoteroApiError.unchanged -> {
                            return SyncError.nonFatal2(NonFatal.unchanged)
                        }
                        is ZoteroApiError.responseMissing -> {
                            return SyncError.nonFatal2(NonFatal.unknown(messageS = "missing response", data = data))
                        }
                    }
                }


                // Check realm errors, every "core" error is bad. Can't create new Realm instance, can't continue with sync
                if (error is RealmError) {
                    Timber.e("received realm error - $error")
                    return SyncError.fatal2(SyncError.Fatal.dbError)
                }
                val schemaError = error as? SchemaError
                if (schemaError != null) {
                    return SyncError.nonFatal2(NonFatal.schema(error = error, data = data))
                }
                val parsingError = error as? Parsing.Error
                if (parsingError != null) {
                    return SyncError.nonFatal2(NonFatal.parsing(error = error, data = data))
                }
                Timber.e("received unknown error - $error")
                return SyncError.nonFatal2(
                    NonFatal.unknown(
                        messageS = error.localizedMessage ?: "", data = data
                    )
                )

            }
            is CustomResult.GeneralError.NetworkError -> {
                if (customResultError.isUnchanged()) {
                    return SyncError.nonFatal2(NonFatal.unchanged)
                }
                return convertNetworkToSyncError(
                    customResultError,
                    response = customResultError.stringResponse ?: "No Response",
                    data = data
                )
            }
        }
    }

    private fun convertNetworkToSyncError(
        error: CustomResult.GeneralError.NetworkError,
        response: String,
        data: SyncError.ErrorData
    ): SyncError {
        if (error.isNoNetworkError()) {
            return SyncError.fatal2(SyncError.Fatal.noInternetConnection)
        }
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
            412 ->
                return SyncError.nonFatal2(NonFatal.preconditionFailed(data.libraryId))
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
            StoreVersionSyncAction(
                version = version,
                type = type,
                libraryId = libraryId
            ).result()
            finishCompletableAction(null)
        }catch (e: Throwable) {
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private suspend fun finishCompletableAction(errorData: Pair<Throwable, SyncError.ErrorData>?) {
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
            val conflicts = PerformDeletionsSyncAction(
                libraryId = libraryId,
                collections = collections,
                items = items,
                searches = searches,
                tags = tags,
                conflictMode = conflictMode
            ).result()
            finishDeletionsSync(result = CustomResult.GeneralSuccess(conflicts), items = items, libraryId = libraryId)
        } catch (e: Throwable) {
            finishDeletionsSync(
                result = CustomResult.GeneralError.CodeError(e),
                items = items,
                libraryId = libraryId
            )
        }
    }

    private suspend fun finishDeletionsSync(
        result: CustomResult<List<Pair<String, String>>>,
        libraryId: LibraryIdentifier,
        items: List<String>? = null,
        version: Int? = null
    ) {
        if (result is CustomResult.GeneralError) {
            val data = items?.let {
                SyncError.ErrorData.from(
                    syncObject = if (!it.isEmpty()) SyncObject.item else SyncObject.collection,
                    keys = it,
                    libraryId = libraryId
                )
            } ?: SyncError.ErrorData.from(libraryId = libraryId)
            val syncError = syncError(
                customResultError = result,
                data = data
            )
            when (syncError) {
                is SyncError.fatal2 ->
                abort(error = syncError.error)
                is SyncError.nonFatal2 ->
                handleNonFatal(error = syncError.error, libraryId = libraryId, version = version)
            }
            return
        }
        result as CustomResult.GeneralSuccess
        val conflicts = result.value!!
        if (!conflicts.isEmpty()) {
            resolve(conflict = Conflict.removedItemsHaveLocalChanges(keys = conflicts, libraryId = libraryId))
        } else {
            processNextAction()
        }
    }

    private fun resolve(conflict: Conflict) {
        conflictEventStream.emitAsync(conflict)
    }

    private suspend fun markChangesAsResolved(libraryId: LibraryIdentifier) {
        try {
            MarkChangesAsResolvedSyncAction(libraryId = libraryId).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private suspend fun markGroupAsLocalOnly(groupId: Int) {
        try {
            MarkGroupAsLocalOnlySyncAction(groupId = groupId).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = LibraryIdentifier.group(groupId)))
        }
    }

    private suspend fun deleteGroup(groupId: Int) {
        try {
            DeleteGroupSyncAction(groupId =  groupId).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = LibraryIdentifier.group(groupId)))
        }

    }

    private suspend fun processCreateUploadActions(
        libraryId: LibraryIdentifier,
        hadOtherWriteActions: Boolean,
        canWriteFiles: Boolean
    ) {
        try {
            val uploads = LoadUploadDataSyncAction(libraryId = libraryId
            ).result()
            process(
                uploads = uploads,
                hadOtherWriteActions = hadOtherWriteActions,
                libraryId = libraryId,
                canWriteFiles = canWriteFiles
            )
        } catch (e: Exception) {
            enqueuedUploads = 0
            uploadsFailedBeforeReachingZoteroBackend = 0

            Timber.e(e)
            finishCompletableAction(e to SyncError.ErrorData.from(libraryId = libraryId))
        }
    }

    private suspend fun process(
        uploads: List<AttachmentUpload>,
        hadOtherWriteActions: Boolean,
        libraryId: LibraryIdentifier,
        canWriteFiles: Boolean
    ) {
        if (uploads.isEmpty()) {
            if (hadOtherWriteActions) {
                processNextAction()
                return
            }

            this.queue.add(index = 0, Action.createLibraryActions(Libraries.specific(listOf(libraryId)), CreateLibraryActionsOptions.onlyDownloads))
            processNextAction()
            return
        }

        if (!canWriteFiles) {
            when (libraryId) {
                is LibraryIdentifier.group -> {
                    val name =
                        dbWrapperMain.realmDbStorage.perform(request = ReadGroupDbRequest(identifier = libraryId.groupId)).name
                    enqueue(
                        actions = listOf(
                            Action.resolveGroupFileWritePermission(
                                groupId = libraryId.groupId,
                                name = name
                            )
                        ), index = 0
                    )

                }

                else -> {
                    //no-op
                }
            }
            return
        }

        this.progressHandler.reportUpload(count = uploads.size)
        enqueuedUploads = uploads.size
        uploadsFailedBeforeReachingZoteroBackend = 0
        enqueue(actions = uploads.map { Action.uploadAttachment(it) }, index = 0)
    }

    private suspend fun finishSubmission(
        error: CustomResult.GeneralError?,
        newVersion: Int?,
        keys: List<String>,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        failedBeforeReachingApi: Boolean = false,
        ignoreWebDav: Boolean = false,
    ) {
        val nextAction = suspend {
            if (newVersion != null) {
                updateVersionInNextWriteBatch(newVersion)
            }
            processNextAction()
        }

         if (error == null) {
            nextAction()
            return
        }

        if (!ignoreWebDav &&
            handleZoteroDirectoryMissing(error, continueExec = {
                finishSubmission(
                    error = error,
                    newVersion = newVersion,
                    keys = keys,
                    libraryId = libraryId,
                    objectS = objectS,
                    failedBeforeReachingApi = failedBeforeReachingApi,
                    ignoreWebDav = true
                )
            })
        ) {
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

                is SyncActionError.authorizationFailed -> {
                    val statusCode = syncActionError.statusCode
                    val response = syncActionError.response
                    handleUploadAuthorizationFailure(
                        statusCode = statusCode,
                        response = response,
                        key = (keys.firstOrNull() ?: ""),
                        libraryId = libraryId,
                        objectS = objectS,
                        newVersion = newVersion,
                        failedBeforeReachingApi = failedBeforeReachingApi
                    )
                    return
                }
                SyncActionError.attachmentItemNotSubmitted -> {
                    val key = keys.firstOrNull()
                    if (key != null) {
                        markItemForUploadAndRestartSync(key = key, libraryId = libraryId)
                        return
                    }
                }
                SyncActionError.objectPreconditionError -> {
                    Timber.e("SyncController: object conflict - trying full sync")
                    abort(error = SyncError.Fatal.uploadObjectConflict(data = SyncError.ErrorData.from(syncObject = objectS, keys = keys, libraryId = libraryId)))
                    return
                }
                else -> {}
            }
        }

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
                    true
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
            else -> {}
        }
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
        this.queue.add(element=Action.createLibraryActions(Libraries.specific(listOf(libraryId)),
            CreateLibraryActionsOptions.onlyDownloads
        ), index = 0)
    }

    //Must be called from SyncScheduler worker thread
    fun cancel() {
        if (!this.isSyncing) {
            return
        }
        Timber.i("Sync: cancelled")
        cleanup()
        report(fatalError = SyncError.Fatal.cancelled)
    }

    private fun report(fatalError: SyncError.Fatal) {
        val sync = requiresRetry(fatalError)
        if (sync != null && this.retryAttempt < this.maxRetryCount) {
            this.observable.emitAsync(sync)
            return
        }

        // Fatal error not retried, report and confirm finished sync.
        this.progressHandler.reportAbort(fatalError)
        this.observable.emitAsync(null)
    }

    private fun requiresRetry(fatalError: SyncError.Fatal): SyncScheduler.Sync? {
        when (fatalError) {
            is SyncError.Fatal.uploadObjectConflict ->
                return SyncScheduler.Sync(
                    type = SyncKind.full,
                    libraries = Libraries.all,
                    retryAttempt = (this.retryAttempt + 1),
                    retryOnce = true
                )

            is SyncError.Fatal.cantSubmitAttachmentItem ->
                return SyncScheduler.Sync(
                    type = this.type,
                    libraries = this.libraryType,
                    retryAttempt = (this.retryAttempt + 1),
                    retryOnce = false
                )

            else ->
                return null
        }
    }

    private fun reportFinish(errors: List<NonFatal>) {
        val q = requireRetry(errors = errors)

        if (q == null || this.retryAttempt >= this.maxRetryCount ) {
            //TODO report finish progress
            this.progressHandler.reportFinish(errors)
            this.observable.emitAsync(null)
            return
        }
        val sync = q.first
        this.progressHandler.reportFinish(q.second)
        this.observable.emitAsync(sync)
    }

    private fun requireRetry(errors: List<NonFatal>): Pair<SyncScheduler.Sync, List<NonFatal>>? {
        // Find libraries which reported version mismatch.
        val retryLibraries = mutableListOf<LibraryIdentifier>()
        val reportErrors = mutableListOf<NonFatal>()
        var retryOnce = false
        var type = this.type

        for (error in errors) {
            when (error) {
                is NonFatal.versionMismatch -> {
                    if (!retryLibraries.contains(error.libraryId)) {
                        retryLibraries.add(error.libraryId)
                    }
                    retryOnce = true
                    type = SyncKind.prioritizeDownloads
                }
                is NonFatal.preconditionFailed -> {
                    if (!retryLibraries.contains(error.libraryId)) {
                        retryLibraries.add(error.libraryId)
                    }
                    retryOnce = true
                    type = SyncKind.prioritizeDownloads
                }
                is NonFatal.annotationDidSplit -> {
                    if (!retryLibraries.contains(error.libraryId)) {
                        retryLibraries.add(error.libraryId)
                    }
                }
                is NonFatal.unknown, is NonFatal.schema, is NonFatal.parsing, is NonFatal.apiError,
                is NonFatal.unchanged, is NonFatal.quotaLimit, is NonFatal.attachmentMissing,
                is NonFatal.insufficientSpace, is NonFatal.webDavDeletion, is NonFatal.webDavDeletionFailed,
                is NonFatal.webDavUpload, is NonFatal.webDavDownload, is NonFatal.webDavVerification ->
                reportErrors.add(error)
            }
        }

        if (retryLibraries.isEmpty()) {
            return null
        }
        return SyncScheduler.Sync(
            type = type,
            libraries = Libraries.specific(retryLibraries),
            retryAttempt = (this.retryAttempt + 1),
            retryOnce = retryOnce
        ) to reportErrors
    }

    fun enqueueResolution(resolution: ConflictResolution) {
        syncSchedulerCoroutineScope.launch {
            enqueue(actions = actions(resolution), index = 0)
        }
    }

    private fun actions(resolution: ConflictResolution): List<Action> {
        when (resolution) {
            is ConflictResolution.deleteGroup -> {
                return listOf(Action.deleteGroup(resolution.id))
            }
            is ConflictResolution.markGroupAsLocalOnly -> {
                return listOf(
                    Action.markGroupAsLocalOnly(
                        resolution.id
                    )
                )
            }
            is ConflictResolution.revertGroupChanges -> {
                return listOf(
                    Action.revertLibraryToOriginal(
                        resolution.id
                    )
                )
            }
            is ConflictResolution.skipGroup -> {
                return listOf(
                    Action.removeActions(
                        resolution.id
                    )
                )
            }
            is ConflictResolution.revertGroupFiles -> {
                return listOf(
                    Action.revertLibraryFilesToOriginal(
                        resolution.id
                    )
                )
            }

            is ConflictResolution.remoteDeletionOfActiveObject -> {
                val actions = mutableListOf<Action>()
                if (!resolution.toDeleteCollections.isEmpty() || !resolution.toDeleteItems.isEmpty() || !resolution.searches.isEmpty() || !resolution.tags.isEmpty()) {
                    actions.add(
                        Action.performDeletions(
                            libraryId = resolution.libraryId,
                            collections = resolution.toDeleteCollections,
                            items = resolution.toDeleteItems,
                            searches = resolution.searches,
                            tags = resolution.tags,
                            conflictMode = PerformDeletionsDbRequest.ConflictResolutionMode.resolveConflicts
                        )
                    )
                }
                if (!resolution.toRestoreCollections.isEmpty() || !resolution.toRestoreItems.isEmpty()) {
                    actions.add(
                        Action.restoreDeletions(
                            libraryId = resolution.libraryId,
                            collections = resolution.toRestoreCollections,
                            items = resolution.toRestoreItems
                        )
                    )
                }
                return actions
            }
            is ConflictResolution.remoteDeletionOfChangedItem -> {
                val actions = mutableListOf<Action>()
                if (!resolution.toDelete.isEmpty()) {
                    actions.add(
                        Action.performDeletions(
                            libraryId = resolution.libraryId,
                            collections = emptyList(),
                            items = resolution.toDelete,
                            searches = emptyList(),
                            tags = emptyList(),
                            conflictMode = PerformDeletionsDbRequest.ConflictResolutionMode.deleteConflicts
                        )
                    )
                }
                if (!resolution.toRestore.isEmpty()) {
                    actions.add(
                        Action.restoreDeletions(
                            libraryId = resolution.libraryId,
                            collections = emptyList(),
                            items = resolution.toRestore
                        )
                    )
                }
                return actions
            }
        }
    }

    private suspend fun loadRemoteDeletions(libraryId: LibraryIdentifier, sinceVersion: Int) {
        val result = LoadDeletionsSyncAction(
            currentVersion = this.lastReturnedVersion,
            sinceVersion = sinceVersion,
            libraryId = libraryId,
            userId = this.userId,
        ).result()
        if (result is CustomResult.GeneralError) {
            finishDeletionsSync(result, libraryId = libraryId, items = null, version = sinceVersion)
            return
        }
        result as CustomResult.GeneralSuccess
        val value = result.value!!
        loadedRemoteDeletions(
            collections = value.collections,
            items = value.items,
            searches = value.searches,
            tags = value.tags,
            version = value.version,
            libraryId = libraryId
        )
    }

    private suspend fun loadedRemoteDeletions(
        collections: List<String>,
        items: List<String>,
        searches: List<String>,
        tags: List<String>,
        version: Int,
        libraryId: LibraryIdentifier
    ) {
        updateDeletionVersion(libraryId, version)

        when (this.type) {
            SyncKind.full ->
                performDeletions(
                    libraryId = libraryId,
                    collections = collections,
                    items = items,
                    searches = searches,
                    tags = tags,
                    conflictMode = PerformDeletionsDbRequest.ConflictResolutionMode.restoreConflicts
                )

            SyncKind.collectionsOnly, SyncKind.ignoreIndividualDelays, SyncKind.normal, SyncKind.keysOnly, SyncKind.prioritizeDownloads ->
                resolve(
                    conflict = Conflict.objectsRemovedRemotely(
                        libraryId = libraryId,
                        collections = collections,
                        items = items,
                        searches = searches,
                        tags = tags
                    )
                )
        }
    }

    private fun updateDeletionVersion(libraryId: LibraryIdentifier, version: Int) {
        for ((idx, action) in this.queue.withIndex()) {
            when(action) {
                is Action.storeDeletionVersion -> {
                    if (action.libraryId != libraryId) {
                        continue
                    }
                    this.queue[idx] =
                        Action.storeDeletionVersion(libraryId = libraryId, version = version)
                    return
                }
                else -> continue
            }
        }
    }

    private suspend fun markGroupForResync(identifier: Int) {
        try {
            MarkGroupForResyncSyncAction(
                identifier = identifier,
            ).result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            Timber.e(e)
            finishCompletableAction(
                errorData = e to SyncError.ErrorData.from(
                    libraryId = LibraryIdentifier.group(
                        identifier
                    )
                )
            )
        }
    }
    private suspend fun processGroupSync(groupId: Int) {
        val action = FetchAndStoreGroupSyncAction(identifier = groupId, userId = this.userId)
        val result = action.result()
        if (result !is CustomResult.GeneralSuccess) {
           val error = result as CustomResult.GeneralError.CodeError
            finishGroupSyncAction(groupId, error.throwable)
            return
        }
        finishGroupSyncAction(groupId, null)
    }

    private suspend fun finishGroupSyncAction(identifier: Int, error: Throwable?) {
        if (error == null) {
            this.progressHandler.reportGroupSynced()
            processNextAction()
            return
        }

        Timber.e(error, "Sync: group failed")
        val e = syncError(
            customResultError = CustomResult.GeneralError.CodeError(error),
            data = SyncError.ErrorData.from(
                libraryId = LibraryIdentifier.group(
                    identifier
                )
            )
        )
        when(e) {
            is SyncError.fatal2 ->  {
                abort(e.error)
            }

            is SyncError.nonFatal2 -> {
                this.nonFatalErrors.add(e.error)
                this.progressHandler.reportGroupSynced()
                markGroupForResync(identifier = identifier)
            }
        }
    }

    private suspend fun processUploadFix(key: String, libraryId: LibraryIdentifier) {
        val action = UploadFixSyncAction(
            key = key,
            libraryId = libraryId,
            userId = this.userId,
            coroutineScope = this.syncSchedulerCoroutineScope,
        )
        try {
            action.result()
            processNextAction()
        } catch (e: Exception) {
            Timber.e(e)
            abort(
                error = SyncError.Fatal.uploadObjectConflict(
                    data = SyncError.ErrorData(
                        itemKeys = listOf(
                            key
                        ), libraryId = libraryId
                    )
                )
            )
        }
    }

    private suspend fun revertGroupFiles(libraryId: LibraryIdentifier) {
        try {
            RevertLibraryFilesSyncAction(
                libraryId = libraryId,
            )
                .result()
            finishCompletableAction(errorData = null)
        } catch (e: Exception) {
            finishCompletableAction(errorData = Pair(e, SyncError.ErrorData.from(libraryId)))
        }
    }

    private suspend fun processSubmitDeletion(batch: DeleteBatch) {
        val actionResult = SubmitDeletionSyncAction(
            keys = batch.keys,
            objectS = batch.objectS,
            version = batch.version,
            libraryId = batch.libraryId,
            userId = this.userId,
            webDavEnabled = sessionStorage.isEnabled,
        ).result()

        if (actionResult !is CustomResult.GeneralSuccess) {
            this.progressHandler.reportWriteBatchSynced(size = batch.keys.size)
            finishSubmission(
                error = actionResult as CustomResult.GeneralError,
                newVersion = batch.version,
                keys = batch.keys,
                libraryId = batch.libraryId,
                objectS = batch.objectS
            )
        } else {
            this.progressHandler.reportWriteBatchSynced(size = batch.keys.size)

            val version = actionResult.value!!.first
            val didCreateDeletions = actionResult.value!!.second

            if (didCreateDeletions) {
                addWebDavDeletionsActionIfNeeded(libraryId = batch.libraryId)
            }
            finishSubmission(
                error = null,
                newVersion = version,
                keys = batch.keys,
                libraryId = batch.libraryId,
                objectS = batch.objectS
            )
        }
    }

    private fun addWebDavDeletionsActionIfNeeded(libraryId: LibraryIdentifier) {
        var libraryIndex = 0
        for (action in this.queue) {
            if (action.libraryId != libraryId) {
                break
            }

            when (action) {
                is Action.performWebDavDeletions -> {
                    // If WebDAV deletions action for this library is already available, don't do anything
                    return
                }

                else -> {
                    libraryIndex += 1

                }
            }
        }
        // Insert deletions action to queue at the end of this library actions
        this.queue.add(element = Action.performWebDavDeletions(libraryId), index = libraryIndex)
    }

    private suspend fun handleUploadAuthorizationFailure(
        statusCode: Int,
        response: String,
        key: String,
        libraryId: LibraryIdentifier,
        objectS: SyncObject,
        newVersion: Int?,
        failedBeforeReachingApi: Boolean
    ) {
        val nonFatalError: NonFatal
        when (statusCode) {
            403 -> {
                when (libraryId) {
                    is LibraryIdentifier.group -> {
                        val groupId = libraryId.groupId
                        val name =
                            dbWrapperMain.realmDbStorage.perform(
                                request = ReadGroupDbRequest
                                    (identifier = groupId)
                            ).name
                        enqueue(
                            actions = listOf(
                                Action.resolveGroupFileWritePermission(
                                    groupId = groupId,
                                    name = name
                                )
                            ), index = 0
                        )
                        return
                    }
                    is LibraryIdentifier.custom -> {
                        nonFatalError =
                            NonFatal.apiError(
                                response = response,
                                data = SyncError.ErrorData.from(
                                    syncObject = objectS,
                                    keys = listOf(key),
                                    libraryId = libraryId
                                )
                            )
                    }
                }
            }
            413 -> {
                nonFatalError = NonFatal.quotaLimit(libraryId)
            }
            404 -> {
                markItemForUploadAndRestartSync(key = key, libraryId = libraryId)
                return
            }
            412 -> {
                Timber.e("SyncController: download remote attachment file and mark attachment as uploaded")
                enqueue(actions = listOf(Action.fixUpload(key = key, libraryId = libraryId)), index = 0)
                return
            }
            else -> {
                nonFatalError = NonFatal.apiError(response = response, data = SyncError.ErrorData.from(syncObject = objectS, keys = listOf(key), libraryId = libraryId))
            }
        }

        handleNonFatal(
            error = nonFatalError,
            libraryId = libraryId,
            version = newVersion,
            additionalAction = {
                if (newVersion != null) {
                    updateVersionInNextWriteBatch(newVersion)
                }
                if (failedBeforeReachingApi) {
                    handleAllUploadsFailedBeforeReachingZoteroBackend(libraryId)
                }
                true
            })
    }

    private fun markItemForUploadAndRestartSync(key: String, libraryId: LibraryIdentifier) {
        try {
            markItemForUpload(key = key, libraryId = libraryId)
            abort(
                error = SyncError.Fatal.cantSubmitAttachmentItem(
                    data =
                    SyncError.ErrorData.from(
                        syncObject = SyncObject.item,
                        keys = listOf(key),
                        libraryId = libraryId
                    )
                )
            )
        } catch (e: Exception) {
            abort(error = SyncError.Fatal.dbError)
        }
    }

    private fun markItemForUpload(key: String, libraryId: LibraryIdentifier) {
        val request = MarkObjectsAsChangedByUser(
            libraryId = libraryId,
            collections = emptyList(),
            items = listOf(key)
        )

        try {
            dbWrapperMain.realmDbStorage.perform(request = request)
        } catch (error: Exception) {
            Timber.e(error, "SyncController: can't mark item for upload")
            throw error
        }
    }

    private suspend fun performWebDavDeletions(libraryId: LibraryIdentifier) {
        val result = DeleteWebDavFilesSyncAction(libraryId = libraryId).result()
        when (result) {
            is CustomResult.GeneralSuccess -> {
                val failures = result.value!!
                if (failures.isEmpty()) {
                    processNextAction()
                } else {
                    handleNonFatal(
                        error = NonFatal.webDavDeletion(
                            count = failures.size,
                            library = libraryId.debugName
                        ), libraryId = libraryId, version = null
                    )
                }
            }

            is CustomResult.GeneralError -> {
                handleWebDavDeletions(error = result, libraryId = libraryId)
            }
        }
    }

    private suspend fun handleWebDavDeletions(error: CustomResult.GeneralError, libraryId: LibraryIdentifier, ignoreWebDav: Boolean = false) {
        if (!ignoreWebDav && handleZoteroDirectoryMissing(error, continueExec = { handleWebDavDeletions(error = error, libraryId = libraryId, ignoreWebDav=  true) })) {
            return
        }
        val localizedDescription = when (error) {
            is CustomResult.GeneralError.CodeError -> {
                error.throwable.localizedMessage!!
            }

            is CustomResult.GeneralError.NetworkError -> {
                error.stringResponse!!
            }
        }
        handleNonFatal(error = NonFatal.webDavDeletionFailed(error = localizedDescription, library = libraryId.debugName), libraryId = libraryId, version = null)

    }

    private suspend fun handleZoteroDirectoryMissing(
        error: CustomResult.GeneralError,
        continueExec: suspend () -> Unit
    ): Boolean {
        val zoteroDirNotFoundError =
            (error as? CustomResult.GeneralError.CodeError)?.throwable as? WebDavError.Verification.zoteroDirNotFound
        if (zoteroDirNotFoundError == null) {
            return false
        }
        //TODO implement WebDav askToCreateZoteroDirectory
        continueExec()
        return true
    }

}
