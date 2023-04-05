package org.zotero.android.sync.conflictresolution

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
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
            is Conflict.groupRemoved -> TODO()
            is Conflict.groupWriteDenied -> TODO()
            is Conflict.objectsRemovedRemotely -> resolveObjectsRemovedRemotely(conflict)
            is Conflict.removedItemsHaveLocalChanges -> TODO()
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

        val key = showsItem(libraryId = libraryId)
        if (key != null && toDeleteItems.contains(key)) {
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
        var resolution = ConflictResolution.remoteDeletionOfActiveObject(
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
}