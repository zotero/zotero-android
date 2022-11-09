package org.zotero.android.dashboard

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import kotlinx.coroutines.launch
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.requests.ReadCollectionDbRequest
import org.zotero.android.architecture.database.requests.ReadItemsDbRequest
import org.zotero.android.architecture.database.requests.ReadLibraryDbRequest
import org.zotero.android.architecture.database.requests.ReadSearchDbRequest
import org.zotero.android.dashboard.data.InitialLoadData
import org.zotero.android.dashboard.data.ItemsError
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.ItemResultsUseCase
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uidata.Collection
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AllItemsViewModel @Inject constructor(
    private val itemResultsUseCase: ItemResultsUseCase,
    private val sdkPrefs: SdkPrefs,
    private val dbWrapper: DbWrapper,
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

    fun init(lifecycleOwner: LifecycleOwner) = initOnce {
        viewModelScope.launch {
            val data = loadInitialDetailData(
                collectionId = sdkPrefs.getSelectedCollectionId(),
                libraryId = sdkPrefs.getSelectedLibrary()
            )
            if (data != null) {
                showItems(data.collection, data.library, searchItemKeys = null)
            }
        }
    }

    private suspend fun loadInitialDetailData(collectionId: CollectionIdentifier, libraryId: LibraryIdentifier): InitialLoadData? {

        var collection: Collection? = null
        var library: Library? = null

        try {
            dbWrapper.realmDbStorage.perform(coordinatorAction = { coordinator ->
                when (collectionId) {
                    is CollectionIdentifier.collection -> {
                        val rCollection = coordinator.perform(request = ReadCollectionDbRequest(libraryId = libraryId, key = collectionId.key))
                        collection = Collection.initWithCollection(objectS = rCollection, itemCount = 0)
                    }
                    is CollectionIdentifier.search -> {
                        val rSearch = coordinator.perform(request = ReadSearchDbRequest(libraryId = libraryId, key = collectionId.key))
                            collection = Collection.initWithSearch(objectS = rSearch)
                    }
                    is CollectionIdentifier.custom -> {
                        collection = Collection.initWithCustomType(type = collectionId.type)
                    }
                }
                library = coordinator.perform(request = ReadLibraryDbRequest(libraryId = libraryId))

            })
        } catch(e: Exception) {
            Timber.e(e, "MainViewController: can't load initial data")
            return null
        }
        if (collection != null && library != null) {
            return InitialLoadData(collection = collection!!, library = library!!)
        }
        return null
    }




    private fun showItems(collection: Collection, library: Library, searchItemKeys: List<String>?) {
        val searchTerm = searchItemKeys?.joinToString(separator = " ")
        init(collection = collection, library = library, searchTerm = searchTerm, error = null)
    }

    fun init(collection: Collection, library: Library, searchTerm: String?, error: ItemsError?) {
        updateState {
            copy(
                collection = collection,
                library = library,
                error = error,
            )
        }
        viewModelScope.launch {
            process(ItemsAction.loadInitialState)
        }
    }

//    fun init(lifecycleOwner: LifecycleOwner) = initOnce {
//        viewModelScope.launch {
//            process(ItemsAction.loadInitialState)
//        }


//        viewModelScope.launch {
//            itemResultsUseCase.resultLiveData.observe(lifecycleOwner) {
//                when (it) {
//                    is CustomResult.GeneralSuccess -> {
//                        if (it.value.isNotEmpty()) {
//                            updateState {
//                                copy(lce = LCE2.Content, items = it.value)
//                            }
//                        }
//                    }
//                    else -> {
//                        updateState {
//                            copy(lce = LCE2.LoadError {})
//                        }
//                    }
//                }
//            }
//        }
//    }

    suspend fun process(action: ItemsAction) {
        when(action) {
            is ItemsAction.loadInitialState -> {
                loadInitialState()
            }
        }
    }

    private suspend fun loadInitialState() {
        val request = ReadItemsDbRequest(collectionId = viewState.collection.identifier, libraryId = viewState.library.identifier, sdkPrefs = sdkPrefs)
        val results = dbWrapper.realmDbStorage.perform(request = request)//TODO sort by descriptors

        updateState {
            copy(
                results = results,
                error = if (results == null) ItemsError.dataLoading else null,
            )
        }
    }

    fun showDefaultLibrary() {
        val libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
        val collectionId = storeIfNeeded(libraryId)
    }

    private fun storeIfNeeded(libraryId: LibraryIdentifier, collectionId: CollectionIdentifier? = null): CollectionIdentifier {
        if (sdkPrefs.getSelectedLibrary() == libraryId) {
            if (collectionId != null) {
                sdkPrefs.setSelectedCollectionId(collectionId)
                return collectionId
            }
            return sdkPrefs.getSelectedCollectionId()
        }

        val collectionId = collectionId ?: CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
        sdkPrefs.setSelectedLibrary(libraryId)
        sdkPrefs.setSelectedCollectionId(collectionId)
        return collectionId

    }

}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val items: List<ItemResponse> = emptyList(),
    val collection: Collection = Collection(identifier = CollectionIdentifier.collection(""), name = "", itemCount = 0),
    val library: Library = Library(LibraryIdentifier.group(0),"",false, false),
    var error: ItemsError? = null,
    var results: RealmResults<RItem>? = null
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
}

sealed class ItemsAction {
    object loadInitialState: ItemsAction()
}
