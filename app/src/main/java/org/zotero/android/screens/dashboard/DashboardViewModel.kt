package org.zotero.android.screens.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.ReadCollectionDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.database.requests.ReadSearchDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.InitialLoadData
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val dbWrapper: DbWrapper,
    private val defaults: Defaults,
    private val fileStore: FileStore
    ) : BaseViewModel2<DashboardViewState, DashboardViewEffect>(DashboardViewState()) {

    fun init() = initOnce {
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
        return null
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
}

internal data class DashboardViewState(
    val snackbarMessage: SnackbarMessage? = null,
) : ViewState

internal sealed class DashboardViewEffect : ViewEffect {
}
