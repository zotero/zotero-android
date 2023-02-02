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
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.Database
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.requests.CreateAttachmentsDbRequest
import org.zotero.android.architecture.database.requests.CreateNoteDbRequest
import org.zotero.android.architecture.database.requests.EditNoteDbRequest
import org.zotero.android.architecture.database.requests.ReadCollectionDbRequest
import org.zotero.android.architecture.database.requests.ReadItemsDbRequest
import org.zotero.android.architecture.database.requests.ReadLibraryDbRequest
import org.zotero.android.architecture.database.requests.ReadSearchDbRequest
import org.zotero.android.architecture.ifFailure
import org.zotero.android.dashboard.data.AddOrEditNoteArgs
import org.zotero.android.dashboard.data.DetailType
import org.zotero.android.dashboard.data.InitialLoadData
import org.zotero.android.dashboard.data.ItemAccessory
import org.zotero.android.dashboard.data.ItemsError
import org.zotero.android.dashboard.data.SaveNoteAction
import org.zotero.android.dashboard.data.ShowItemDetailsArgs
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.MediaSelectionResult
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.helpers.UriExtractor
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncUseCase
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uidata.Collection
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AllItemsViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val dispatcher: CoroutineDispatcher,
    private val uriExtractor: UriExtractor,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
    private val schemaController: SchemaController,
    private val syncUseCase: SyncUseCase
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(saveNoteAction: SaveNoteAction) {
        if (saveNoteAction.isFromDashboard) {
            viewModelScope.launch {
                saveNote(saveNoteAction.text, saveNoteAction.tags, saveNoteAction.key)
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        viewModelScope.launch {
            val data = loadInitialDetailData(
                collectionId = defaults.getSelectedCollectionId(),
                libraryId = defaults.getSelectedLibrary()
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
                ItemsAction.startSync -> TODO()
                is ItemsAction.updateKeys -> {
                    processUpdate(items = action.items, deletions = action.deletions, insertions = action.insertions, modifications = action.modifications)
                }
            }
        }
    }

    private fun processUpdate(
        items: RealmResults<RItem>,
        deletions: IntArray,
        insertions: IntArray,
        modifications: IntArray
    ) {
        if (viewState.isEditing) {
            deletions.sorted().reversed().forEach { idx ->
               val mutableKeys = viewState.keys.toMutableList()
                val key = mutableKeys.removeAt(idx)
                updateState {
                    copy(keys = mutableKeys)
                }


                val mutableSelectedItems = viewState.selectedItems.toMutableSet()
                val isSelectedRemoved = mutableSelectedItems.remove(key)
                updateState {
                    copy(selectedItems = mutableSelectedItems)
                }
                if (isSelectedRemoved) {
                    updateState {
                        copy(changes = viewState.changes + Changes.selection)
                    }
                }
            }
        }
        modifications.forEach { idx ->
            val item = items[idx]
            val mutableItemAccessories = viewState.itemAccessories.toMutableMap()
            val itemAccessory = accessory(item!!)
            if (itemAccessory != null) {
                mutableItemAccessories.put(item.key, itemAccessory)
                updateState {
                    copy(itemAccessories = mutableItemAccessories)
                }
            }

        }
        insertions.forEach { idx ->
            val item = items[idx]
            if (viewState.isEditing) {
                val mutableKeys = viewState.keys.toMutableList()
                mutableKeys.add(element = item!!.key, index = idx)
                updateState {
                    copy(keys = mutableKeys)
                }
            }

            val mutableItemAccessories = viewState.itemAccessories.toMutableMap()
            val itemAccessory = accessory(item!!)
            if (itemAccessory != null) {
                mutableItemAccessories.put(item.key, itemAccessory)
                updateState {
                    copy(itemAccessories = mutableItemAccessories)
                }
            }
        }

    }

    private fun accessory(item: RItem): ItemAccessory? {
        val attachment = AttachmentCreator.mainAttachment(item, fileStorage = fileStore)
        if (attachment != null) {
            return ItemAccessory.attachment(attachment)
        }
        val urlString = item.urlString
        if (urlString != null && UrlDetector().isUrl(urlString)) {
            return ItemAccessory.url(urlString)
        }
        val doi = item.doi
        if (doi != null) {
            return ItemAccessory.doi(doi)
        }

        return null
    }

    private suspend fun loadInitialState() {
        val request = ReadItemsDbRequest(collectionId = viewState.collection.identifier, libraryId = viewState.library.identifier, defaults = defaults)
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
        if (defaults.getSelectedLibrary() == libraryId) {
            if (collectionId != null) {
                defaults.setSelectedCollectionId(collectionId)
                return collectionId
            }
            return defaults.getSelectedCollectionId()
        }

        val collectionId = collectionId ?: CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
        defaults.setSelectedLibrary(libraryId)
        defaults.setSelectedCollectionId(collectionId)
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
            val key = KeyGenerator.newKey()

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
        val type = schemaController.localizedItemType(ItemTypes.attachment) ?: ""
        val request = CreateAttachmentsDbRequest(attachments = attachments, parentKey = null, localizedType = type, collections = collections, fileStore = fileStore)

        val result = perform(dbWrapper, invalidateRealm = true, request = request).ifFailure {
            Timber.e(it,"ItemsActionHandler: can't add attachment")
            updateState {
                copy(error = ItemsError.attachmentAdding(ItemsError.AttachmentLoading.couldNotSave))
            }
            return
        }
        val failed = result
        if (failed.isEmpty()) {
            return
        }
        updateState {
            copy(error = ItemsError.attachmentAdding(ItemsError.AttachmentLoading.someFailed(failed.map { it.second })))
        }

    }

    fun onAddNote() {
        showNoteCreation(title = null, libraryId = viewState.library.identifier)
    }

    private fun showNoteCreation(title: AddOrEditNoteArgs.TitleData?, libraryId: LibraryIdentifier) {
        ScreenArguments.addOrEditNoteArgs = AddOrEditNoteArgs(
            text = "",
            tags = listOf(),
            title = title,
            key = KeyGenerator.newKey(),
            libraryId = libraryId,
            readOnly = false,
            isFromDashboard = true,
        )
        triggerEffect(AllItemsViewEffect.ShowAddOrEditNoteEffect)
    }

    fun showItemDetail(item: RItem) {
        when (item.rawType) {
            ItemTypes.note -> {
                val note = Note.init(item = item)
                if (note == null) {
                    return
                }
                val tags = item.tags!!.map({ Tag(tag = it) })
                val library = viewState.library
                ScreenArguments.addOrEditNoteArgs = AddOrEditNoteArgs(
                    text = note.text,
                    tags = tags,
                    title = null,
                    libraryId = library.identifier,
                    readOnly = !library.metadataEditable,
                    key = note.key,
                    isFromDashboard = true
                )
                triggerEffect(AllItemsViewEffect.ShowAddOrEditNoteEffect)
            }
            else -> {
                ScreenArguments.showItemDetailsArgs = ShowItemDetailsArgs(DetailType.preview(key = item.key), library = viewState.library, childKey = null)
                triggerEffect(AllItemsViewEffect.ShowItemDetailEffect)
            }
        }
    }

    private suspend fun saveNote(text: String, tags: List<Tag>, key: String) = withContext(dispatcher) {
        val note = Note(key = key, text = text, tags = tags)
        val libraryId = viewState.library.identifier
        var collectionKey: String? = null

        val identifier = viewState.collection.identifier
        when (identifier) {
            is CollectionIdentifier.collection ->
                collectionKey = identifier.key
            is CollectionIdentifier.custom, is CollectionIdentifier.search ->
                collectionKey = null
        }

        try {
            dbWrapper.realmDbStorage.perform(
                EditNoteDbRequest(
                    note = note,
                    libraryId = libraryId
                )
            )
        } catch (e: Throwable) {
            if (e is DbError.objectNotFound) {
                val request = CreateNoteDbRequest(
                    note = note,
                    localizedType = (schemaController.localizedItemType(
                        ItemTypes.note
                    ) ?: ""),
                    libraryId = libraryId,
                    collectionKey = collectionKey,
                    parentKey = null
                )
                dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
            } else {
                Timber.e(e)
            }
        }
    }
}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val tableItems: RealmResults<RItem>? = null,
    val collection: Collection = Collection(identifier = CollectionIdentifier.collection(""), name = "", itemCount = 0),
    val library: Library = Library(LibraryIdentifier.group(0),"",false, false),
    var itemAccessories: Map<String, ItemAccessory> = emptyMap(),
    var keys: List<String> = emptyList(),
    var selectedItems: Set<String> = emptySet(),
    var isEditing: Boolean = false,
    var changes: List<Changes> = emptyList(),
    var error: ItemsError? = null,
    var results: RealmResults<RItem>? = null,
    val shouldShowAddBottomSheet: Boolean = false
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
    object ShowItemDetailEffect: AllItemsViewEffect()
    object ShowAddOrEditNoteEffect: AllItemsViewEffect()
}

sealed class ItemsAction {
    object loadInitialState: ItemsAction()
    object observingFailed: ItemsAction()
    data class updateKeys(val items: RealmResults<RItem>, val deletions: IntArray, val insertions: IntArray, val modifications: IntArray): ItemsAction()
    object startSync: ItemsAction()
}

enum class Changes {
    results,
    editing,
    selection,
    selectAll,
    attachmentsRemoved,
    filters,
    webViewCleanup,
    batchData,
}
