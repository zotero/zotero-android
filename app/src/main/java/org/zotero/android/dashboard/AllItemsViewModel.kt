package org.zotero.android.dashboard

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.Database
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.requests.ReadCollectionDbRequest
import org.zotero.android.architecture.database.requests.ReadItemsDbRequest
import org.zotero.android.architecture.database.requests.ReadLibraryDbRequest
import org.zotero.android.architecture.database.requests.ReadSearchDbRequest
import org.zotero.android.dashboard.data.InitialLoadData
import org.zotero.android.dashboard.data.ItemsError
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.MediaSelectionResult
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.helpers.UriExtractor
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.ItemResultsUseCase
import org.zotero.android.sync.KeyGenerator
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
    private val dispatcher: CoroutineDispatcher,
    private val uriExtractor: UriExtractor,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

    fun init() = initOnce {
        EventBus.getDefault().register(this)
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

    fun process(action: ItemsAction) {
        viewModelScope.launch {
            when(action) {
                is ItemsAction.loadInitialState -> {
                    loadInitialState()
                }
                is ItemsAction.observingFailed -> {
                    updateState {
                        copy(error = ItemsError.dataLoading)
                    }
                }
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
                tableItems = results.freeze(), lce = LCE2.Content,
            )
        }
        startObserving(results)
    }

    private suspend fun startObserving(results: RealmResults<RItem>) {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> {items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                  updateState {
                      copy(tableItems = items.freeze(), lce = LCE2.Content,)
                  }
                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    val correctedModifications = Database.correctedModifications(modifications = modifications, insertions = insertions, deletions = deletions)
                    process(ItemsAction.updateKeys(items = items, deletions = deletions, insertions = insertions, modifications = correctedModifications))
                    updateState {
                        copy(tableItems = items.freeze(), lce = LCE2.Content,)
                    }

                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "ItemsViewController: could not load results")
                    process(ItemsAction.observingFailed)
                    updateState {
                        copy(lce = LCE2.LoadError {})
                    }
                }
            }
        })

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

    private fun startSync() {
        process(action = ItemsAction.startSync)
    }

    fun onAdd() {
        updateState {
            copy(
                shouldShowAddBottomSheet = true
            )
        }
    }

    fun onAddFile() {
        onAddBottomSheetCollapse()
    }

    fun onAddBottomSheetCollapse() {
        updateState {
            copy(
                shouldShowAddBottomSheet = false
            )
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: EventBusConstants.FileWasSelected) {
        if (event.uri != null) {
            viewModelScope.launch {
                addAttachments(listOf(event.uri))
            }
        }
    }

    private suspend fun addAttachments(urls: List<Uri>) {
        val libraryId = viewState.library.identifier
        val attachments = mutableListOf<Attachment>()
        for (url in urls) {
            val key = KeyGenerator.newKey

            val selectionResult = selectMedia.execute(
                uri = url.toString(),
                isValidMimeType = { true }
            )

            val isSuccess = selectionResult is MediaSelectionResult.AttachMediaSuccess
            if (!isSuccess) {
                //TODO parse errors
                continue
            }
            selectionResult as MediaSelectionResult.AttachMediaSuccess

            val original = selectionResult.file.file
            val filename = original.nameWithoutExtension + "." + original.extension
            val contentType =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(original.extension)!!
            val file = fileStore.attachmentFile(libraryId = libraryId, key = key, filename = filename, contentType = contentType)
            if (!original.renameTo(file)) {
                Timber.e("can't move file")
                continue
            }
            attachments.add(
                Attachment(
                    type = Attachment.Kind.file(
                        filename = filename,
                        contentType = contentType,
                        location = Attachment.FileLocation.local,
                        linkType = Attachment.FileLinkType.importedFile
                    ),
                    title = filename,
                    key = key,
                    libraryId = libraryId
                )
            )
        }
        if (attachments.isEmpty()) {
            updateState {
                copy(error = ItemsError.attachmentAdding(ItemsError.AttachmentLoading.couldNotSave))
            }
            return
        }
        val collections: Set<String>
        val identifier = viewState.collection.identifier
        when(identifier) {
            is CollectionIdentifier.collection -> {
                collections = setOf(identifier.key)
            }
            is CollectionIdentifier.search, is CollectionIdentifier.custom -> {
                collections = emptySet()
            }
        }
        //TODO implement remaining functionality
    }

}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val tableItems: RealmResults<RItem>? = null,
    val collection: Collection = Collection(identifier = CollectionIdentifier.collection(""), name = "", itemCount = 0),
    val library: Library = Library(LibraryIdentifier.group(0),"",false, false),
    var error: ItemsError? = null,
    var results: RealmResults<RItem>? = null,
    val shouldShowAddBottomSheet: Boolean = false
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
}

sealed class ItemsAction {
    object loadInitialState: ItemsAction()
    object observingFailed: ItemsAction()
    data class updateKeys(val items: RealmResults<RItem>, val deletions: IntArray, val insertions: IntArray, val modifications: IntArray): ItemsAction()
    object startSync: ItemsAction()
}
