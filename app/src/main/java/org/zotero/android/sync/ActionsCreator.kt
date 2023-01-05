package org.zotero.android.sync

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionsCreator @Inject constructor() {

    fun createInitialActions(libraries: LibrarySyncType, syncType: SyncType): List<Action> {
        if (SyncType.keysOnly == syncType) {
            return listOf(Action.loadKeyPermissions)
        }

        when (libraries) {
            is LibrarySyncType.all ->
                return listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
            is LibrarySyncType.specific -> {
                for (identifier in libraries.identifiers) {
                    if (identifier is LibraryIdentifier.group) {
                        return listOf(Action.loadKeyPermissions, Action.syncGroupVersions)
                    }
                }
                val options = libraryActionsOptions(syncType)
                return listOf(
                    Action.loadKeyPermissions,
                    Action.createLibraryActions(libraries, options)
                )

            }
        }
    }

    fun libraryActionsOptions(syncType: SyncType): CreateLibraryActionsOptions {
        return when (syncType) {
            SyncType.full, SyncType.collectionsOnly ->
                CreateLibraryActionsOptions.onlyDownloads
            SyncType.ignoreIndividualDelays, SyncType.normal, SyncType.keysOnly ->
                CreateLibraryActionsOptions.automatic
        }
    }

    fun createLibraryActions(
        data: List<LibraryData>,
        creationOptions: CreateLibraryActionsOptions,
        type: SyncType
    ): Triple<List<Action>, Int?, Int> {
        var writeCount = 0
        var actions = mutableListOf<Action>()

        for (libraryData in data) {
            val (_actions, _writeCount) = createLibraryActions(
                libraryData = libraryData,
                creationOptions = creationOptions,
                type = type
            )
            writeCount += _writeCount
            _actions.forEach {
                actions.add(it)
            }
        }

        val index: Int? = if (creationOptions == CreateLibraryActionsOptions.automatic) null else 0
        return Triple(actions, index, writeCount)
    }

    private fun createLibraryActions(
        libraryData: LibraryData,
        creationOptions: CreateLibraryActionsOptions,
        type: SyncType
    ): Pair<List<Action>, Int> {
        when (creationOptions) {
            CreateLibraryActionsOptions.onlyDownloads -> {
                val actions =
                    createDownloadActions(libraryData.identifier, versions = libraryData.versions, type = type)
                return actions to 0
            }


            CreateLibraryActionsOptions.onlyWrites -> {
                var actions = mutableListOf<Action>()
                var writeCount = 0

                if (!libraryData.updates.isEmpty() || !libraryData.deletions.isEmpty() || libraryData.hasUpload) {
                    val (_actions, _writeCount) = createLibraryWriteActions(libraryData)
                    actions = _actions.toMutableList()
                    writeCount = _writeCount
                }

                if (libraryData.hasWebDavDeletions) {
                    actions.add(Action.performWebDavDeletions(libraryData.identifier))
                }

                return actions to writeCount
            }


            CreateLibraryActionsOptions.automatic -> {
                var actions: MutableList<Action>
                var writeCount = 0

                if (!libraryData.updates.isEmpty() || !libraryData.deletions.isEmpty() || libraryData.hasUpload) {
                    val (_actions, _writeCount) = this.createLibraryWriteActions(libraryData)
                    actions = _actions.toMutableList()
                    writeCount = _writeCount
                } else {
                    actions = createDownloadActions(
                        libraryData.identifier,
                        versions = libraryData.versions,
                        type = type
                    ).toMutableList()
                }

                if (libraryData.hasWebDavDeletions) {
                    actions.add(Action.performWebDavDeletions(libraryData.identifier))
                }

                return actions to writeCount
            }

        }
    }

    private fun createLibraryWriteActions(libraryData: LibraryData): Pair<List<Action>, Int> {
        when (libraryData.identifier) {
            is LibraryIdentifier.custom -> {
                val actions = createUpdateActions(
                    updates = libraryData.updates,
                    deletions = libraryData.deletions,
                    libraryId = libraryData.identifier
                )
                return actions to actions.size - 1
            }

            is LibraryIdentifier.group -> {
                if (!libraryData.canEditMetadata) {
                    return listOf(
                        Action.resolveGroupMetadataWritePermission(
                            libraryData.identifier.groupId,
                            libraryData.name
                        )
                    ) to 0
                }
                val actions = createUpdateActions(
                    updates = libraryData.updates,
                    deletions = libraryData.deletions,
                    libraryId = libraryData.identifier
                )
                return actions to actions.size - 1
            }
        }
    }

    private fun createDownloadActions(
        libraryId: LibraryIdentifier,
        versions: Versions,
        type: SyncType
    ): List<Action> {
        when (type) {
            SyncType.keysOnly -> return listOf()
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
            val upperBound = Integer.min((keys.size - lowerBound), batchSize) + lowerBound
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
                batchSize = Integer.min(batchSize * 2, maxBatchSize)
            }
        }

        return batches
    }

    fun createBatchedObjectActions(
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

    fun createGroupActions(
        updateIds: List<Int>,
        deleteGroups: List<Pair<Int, String>>,
        syncType: SyncType,
        libraryType: LibrarySyncType
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
        val options: CreateLibraryActionsOptions = libraryActionsOptions(syncType)
        actions.add(Action.createLibraryActions(libraryType, options))
        return actions
    }


}