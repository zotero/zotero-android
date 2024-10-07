package org.zotero.android.sync.conflictresolution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.screens.dashboard.ConflictDialogData
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictResolutionUseCase @Inject constructor(
    private val syncUseCase: SyncUseCase,
    private val dispatcher: CoroutineDispatcher,
    private val conflictEventStream: ConflictEventStream) {
    private var coroutineScope = CoroutineScope(dispatcher)

    init {
        conflictEventStream.flow()
            .onEach { conflict ->
                resolve(conflict)
            }
            .launchIn(coroutineScope)
    }

    private fun resolve(conflict: Conflict) {
        when (conflict) {
            is Conflict.groupRemoved, is Conflict.groupMetadataWriteDenied, is Conflict.groupFileWriteDenied -> {
                EventBus.getDefault().post(ShowSimpleConflictResolutionDialog(conflict))
            }
            is Conflict.objectsRemovedRemotely -> {
                resolveObjectsRemovedRemotely(conflict)
            }
            is Conflict.removedItemsHaveLocalChanges -> {
                resolveRemovedItemsHaveLocalChanges(conflict)
            }
        }
    }

    var currentlyDisplayedItemLibraryIdentifier: LibraryIdentifier? = null
    var currentlyDisplayedItemKey: String? = null

    private lateinit var libraryId: LibraryIdentifier
    private lateinit var toDeleteCollections: MutableList<String>
    private lateinit var toDeleteItems: MutableList<String>
    private lateinit var toRestoreCollections: MutableList<String>
    private lateinit var toRestoreItems: MutableList<String>

    private lateinit var searches: List<String>
    private lateinit var tags: List<String>

    private fun resolveObjectsRemovedRemotely(conflict: Conflict.objectsRemovedRemotely) {
        libraryId = conflict.libraryId
        toDeleteCollections = conflict.collections.toMutableList()
        toDeleteItems = conflict.items.toMutableList()
        toRestoreCollections = mutableListOf()
        toRestoreItems = mutableListOf()
        searches = conflict.searches
        tags = conflict.tags

        val keyForShowingCollection = showsCollection(libraryId = libraryId)
        val keyForShowingItem = showsItem(libraryId = libraryId)

        if (keyForShowingCollection != null && toDeleteCollections.contains(keyForShowingCollection)) {
            EventBus.getDefault().post(AskUserToDeleteOrRestoreCollection)
        } else if (keyForShowingItem != null && toDeleteItems.contains(keyForShowingItem)) {
            EventBus.getDefault().post(AskUserToDeleteOrRestoreItem)
        } else {
            completeResolveObjectsRemovedRemotely()
        }

    }
    private fun showsItem(libraryId: LibraryIdentifier): String? {
        if (libraryId == currentlyDisplayedItemLibraryIdentifier) {
            return currentlyDisplayedItemKey
        }
        return null
    }

    fun deleteOrRestoreItem(isDelete: Boolean, key: String) {
        if (!isDelete) {
            toRestoreItems.add(key)
            toDeleteItems.remove(key)
        }
        completeResolveObjectsRemovedRemotely()
    }

    private fun completeResolveObjectsRemovedRemotely() {
        val resolution = ConflictResolution.remoteDeletionOfActiveObject(
            libraryId = libraryId,
            toDeleteCollections = toDeleteCollections,
            toDeleteItems = toDeleteItems,
            toRestoreCollections = toRestoreCollections,
            toRestoreItems = toRestoreItems,
            searches = searches,
            tags = tags,
        )
        syncUseCase.enqueueResolution(resolution)
    }

    var currentlyDisplayedCollectionLibraryIdentifier: LibraryIdentifier? = null
    var currentlyDisplayedCollectionKey: String? = null

    private fun showsCollection(libraryId: LibraryIdentifier): String? {
        if (libraryId == currentlyDisplayedCollectionLibraryIdentifier) {
            return currentlyDisplayedCollectionKey
        }
        return null
    }
    fun deleteOrRestoreCollection(isDelete: Boolean) {
        if (!isDelete) {
            val key = currentlyDisplayedCollectionKey!!
            toRestoreCollections.add(key)
            toDeleteCollections.remove(key)
        }
        completeResolveObjectsRemovedRemotely()
    }

    private lateinit var toDelete: MutableList<String>
    private lateinit var toRestore: MutableList<String>

    private fun resolveRemovedItemsHaveLocalChanges(conflict: Conflict.removedItemsHaveLocalChanges) {
        this.libraryId = conflict.libraryId
        toDelete = mutableListOf()
        toRestore = mutableListOf()
        val conflictDataList = conflict.keys.map {
            ConflictDialogData.changedItemsDeletedAlert(
                key = it.first,
                title = it.second
            )
        }
        EventBus.getDefault().post(AskUserToResolveChangedDeletedItem(conflictDataList))
    }

    fun restoreRemovedItemsWithLocalChanges(key: String) {
        toRestore.add(key)
    }

    fun deleteRemovedItemsWithLocalChanges(key: String) {
        toDelete.add(key)
    }

    fun completeRemovedItemsWithLocalChanges() {
        syncUseCase.enqueueResolution(
            ConflictResolution.remoteDeletionOfChangedItem(
                libraryId = libraryId,
                toDelete = this.toDelete,
                toRestore = this.toRestore
            )
        )
    }

    fun deleteGroup(key: Int) {
        syncUseCase.enqueueResolution(ConflictResolution.deleteGroup(key))
    }

    fun markGroupAsLocalOnly(key: Int) {
        syncUseCase.enqueueResolution(ConflictResolution.markGroupAsLocalOnly(key))
    }

    fun revertGroupChanges(key: Int) {
        syncUseCase.enqueueResolution(ConflictResolution.revertGroupChanges(
            LibraryIdentifier.group(
                key
            )
        ))
    }

    fun revertGroupFiles(id: LibraryIdentifier) {
        syncUseCase.enqueueResolution(ConflictResolution.revertGroupFiles(id))
    }
    fun skipGroup(id: LibraryIdentifier) {
        syncUseCase.enqueueResolution(ConflictResolution.skipGroup(id))
    }
}