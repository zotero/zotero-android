package org.zotero.android.screens.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadCollectionDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.database.requests.ReadSearchDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.InitialLoadData
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.dashboard.data.ShowDashboardLongPressBottomSheet
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.conflictresolution.AskUserToResolveChangedDeletedItem
import org.zotero.android.sync.conflictresolution.Conflict
import org.zotero.android.sync.conflictresolution.ConflictResolutionUseCase
import org.zotero.android.sync.conflictresolution.ShowSimpleConflictResolutionDialog
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionsHolder
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val dbWrapper: DbWrapper,
    private val defaults: Defaults,
    private val fileStore: FileStore,
    private val conflictResolutionUseCase: ConflictResolutionUseCase,
) : BaseViewModel2<DashboardViewState, DashboardViewEffect>(DashboardViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AskUserToResolveChangedDeletedItem) {
        updateState {
            copy(changedItemsDeletedAlertQueue = event.conflictDataList)
        }
    }

    @Subscribe
    fun onEvent(event: ShowDashboardLongPressBottomSheet) {
        updateState {
            copy(
                longPressOptionsHolder = LongPressOptionsHolder(
                    title = event.title,
                    longPressOptionItems = event.longPressOptionItems
                )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowSimpleConflictResolutionDialog) {
        updateState {
            val conflict = event.conflict
            copy(
                conflictDialog = when (conflict) {
                    is Conflict.groupRemoved -> {
                        ConflictDialogData.groupRemoved(
                            conflict.groupId,
                            conflict.groupName
                        )
                    }
                    is Conflict.groupWriteDenied -> {
                        ConflictDialogData.groupWriteDenied(
                            conflict.groupId,
                            conflict.groupName
                        )
                    }
                    else -> {
                        null
                    }
                }
            )
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        ScreenArguments.collectionsArgs = CollectionsArgs(libraryId = defaults.getSelectedLibrary(), fileStore.getSelectedCollectionId())
        val data = loadInitialDetailData(
            collectionId = fileStore.getSelectedCollectionId(),
            libraryId = defaults.getSelectedLibrary()
        )
        if (data != null) {
            showItems(data.collection, data.library, searchItemKeys = null)
        }
    }

    private fun loadInitialDetailData(
        collectionId: CollectionIdentifier,
        libraryId: LibraryIdentifier
    ): InitialLoadData? {
        var collection: Collection? = null
        var library: Library? = null

        try {
            dbWrapper.realmDbStorage.perform(coordinatorAction = { coordinator ->
                when (collectionId) {
                    is CollectionIdentifier.collection -> {
                        val rCollection = coordinator.perform(
                            request = ReadCollectionDbRequest(
                                libraryId = libraryId,
                                key = collectionId.key
                            )
                        )
                        collection =
                            Collection.initWithCollection(objectS = rCollection, itemCount = 0)
                    }
                    is CollectionIdentifier.search -> {
                        val rSearch = coordinator.perform(
                            request = ReadSearchDbRequest(
                                libraryId = libraryId,
                                key = collectionId.key
                            )
                        )
                        collection = Collection.initWithSearch(objectS = rSearch)
                    }
                    is CollectionIdentifier.custom -> {
                        collection = Collection.initWithCustomType(type = collectionId.type)
                    }
                }
                library = coordinator.perform(request = ReadLibraryDbRequest(libraryId = libraryId))

            })
        } catch (e: Exception) {
            Timber.e(e, "DashboardScreen: can't load initial data")
            return null
        }
        if (collection != null && library != null) {
            return InitialLoadData(collection = collection!!, library = library!!)
        }
        Timber.w("returning default library and collection")
        return InitialLoadData(
            collection = Collection.initWithCustomType(type = CollectionIdentifier.CustomType.all),
            library = Library(
                identifier = LibraryIdentifier.custom(type = RCustomLibraryType.myLibrary),
                name = "My Library",
                metadataEditable = true,
                filesEditable = true
            )
        )
    }

    private fun showItems(collection: Collection, library: Library, searchItemKeys: List<String>?) {
        val searchTerm = searchItemKeys?.joinToString(separator = " ")
        ScreenArguments.allItemsArgs = AllItemsArgs(
            collection = collection,
            library = library,
            sortType = ItemsSortType.default,
            searchTerm = searchTerm,
            error = null
        )
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun deleteRemovedItemsWithLocalChanges(key: String) {
        conflictResolutionUseCase.deleteRemovedItemsWithLocalChanges(key)
        maybeCompleteRemovedItemsWithLocalChanges(key)
    }

    fun restoreRemovedItemsWithLocalChanges(key: String) {
        conflictResolutionUseCase.restoreRemovedItemsWithLocalChanges(key)
        maybeCompleteRemovedItemsWithLocalChanges(key)
    }

    private fun maybeCompleteRemovedItemsWithLocalChanges(key: String) {
        updateState {
            copy(changedItemsDeletedAlertQueue = viewState.changedItemsDeletedAlertQueue.filter { it.key != key })
        }
        if (viewState.changedItemsDeletedAlertQueue.isEmpty()) {
            conflictResolutionUseCase.completeRemovedItemsWithLocalChanges()
        }
    }

    fun deleteGroup(key: Int) {
        conflictResolutionUseCase.deleteGroup(key)
    }
    fun markGroupAsLocalOnly(key: Int) {
        conflictResolutionUseCase.markGroupAsLocalOnly(key)
    }
    fun revertGroupChanges(key: Int) {
        conflictResolutionUseCase.revertGroupChanges(key)
    }
    fun keepGroupChanges(key: Int) {
        conflictResolutionUseCase.keepGroupChanges(key)
    }

    fun onDismissConflictDialog() {
        updateState {
            copy(
                conflictDialog = null,
            )
        }
    }
    fun dismissBottomSheet() {
        updateState {
            copy(longPressOptionsHolder = null)
        }
    }

    fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        EventBus.getDefault().post(longPressOptionItem)
    }
}

internal data class DashboardViewState(
    val snackbarMessage: SnackbarMessage? = null,
    val conflictDialog: ConflictDialogData? = null,
    val changedItemsDeletedAlertQueue: List<ConflictDialogData.changedItemsDeletedAlert> = emptyList(),
    val longPressOptionsHolder: LongPressOptionsHolder? = null,
    ) : ViewState

internal sealed class DashboardViewEffect : ViewEffect {
}

sealed class ConflictDialogData  {
    data class groupRemoved(val groupId: Int, val groupName: String): ConflictDialogData()
    data class groupWriteDenied(val groupId: Int, val groupName: String): ConflictDialogData()
    data class changedItemsDeletedAlert(val title: String, val key: String): ConflictDialogData()
}