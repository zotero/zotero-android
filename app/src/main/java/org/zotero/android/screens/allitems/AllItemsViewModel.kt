package org.zotero.android.screens.allitems

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.ifFailure
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.database.Database
import org.zotero.android.database.DbError
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.CreateAttachmentsDbRequest
import org.zotero.android.database.requests.CreateNoteDbRequest
import org.zotero.android.database.requests.EditNoteDbRequest
import org.zotero.android.database.requests.EmptyTrashDbRequest
import org.zotero.android.database.requests.MarkItemsAsTrashedDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.ReadItemsDbRequest
import org.zotero.android.database.requests.itemSearch
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetMimeTypeUseCase
import org.zotero.android.helpers.MediaSelectionResult
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.allitems.AllItemsViewEffect.ShowItemTypePickerEffect
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.ItemAccessory
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.allitems.data.ItemsState
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.dashboard.data.ShowDashboardLongPressBottomSheet
import org.zotero.android.screens.filter.data.FilterArgs
import org.zotero.android.screens.filter.data.FilterResult
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.screens.itemdetails.data.ItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.screens.sortpicker.data.SortDirectionResult
import org.zotero.android.screens.sortpicker.data.SortPickerArgs
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.AttachmentFileCleanupController
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.LibrarySyncType
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.sync.SyncType
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerStateCreator
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class AllItemsViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
    private val fileDownloader: AttachmentDownloader,
    private val getMimeTypeUseCase: GetMimeTypeUseCase,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    private val schemaController: SchemaController,
    private val dispatchers: Dispatchers,
    private val fileCleanupController: AttachmentFileCleanupController,
    private val syncScheduler: SyncScheduler
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

    val itemAccessories = mutableMapOf<String, ItemAccessory> ()
    var keys = mutableListOf<String>()
    var results: RealmResults<RItem>? = null
    var filters: MutableList<ItemsState.Filter> = mutableListOf()

    private val onSearchStateFlow = MutableStateFlow("")

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: EventBusConstants.FileWasSelected) {
        if (event.uri != null && event.callPoint == EventBusConstants.FileWasSelected.CallPoint.AllItems) {
            viewModelScope.launch {
                addAttachments(listOf(event.uri))
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(saveNoteAction: SaveNoteAction) {
        if (saveNoteAction.isFromDashboard) {
            viewModelScope.launch {
                saveNote(saveNoteAction.text, saveNoteAction.tags, saveNoteAction.key)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(sortDirectionResult: SortDirectionResult) {
        setSortOrder(sortDirectionResult.isAscending)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(filterResult: FilterResult) {
        when (filterResult) {
            is FilterResult.enableFilter -> {
                enable(filterResult.filter)
            }
            is FilterResult.disableFilter -> {
                disable(filterResult.filter)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LongPressOptionItem) {
        onLongPressOptionsItemSelected(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItemsShowItem) {
            val collectionKey: String?
            val identifier = viewState.collection.identifier
            when (identifier) {
                is CollectionIdentifier.collection ->
                    collectionKey = identifier.key
                is CollectionIdentifier.search, is CollectionIdentifier.custom ->
                    collectionKey = null
            }

            val type = singlePickerResult.id
            viewModelScope.launch {
                delay(800)
                showItemDetail(
                    DetailType.creation(
                        type = type,
                        child = null,
                        collectionKey = collectionKey
                    ), library = viewState.library
                )
            }
        } else if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItemsSortPicker) {
            onSortFieldChanged(singlePickerResult.id)
        }
    }

    private fun onSortFieldChanged(id: String) {
        val field = ItemsSortType.Field.values().first { it.titleStr == id }
        val sortType = viewState.sortType.copy(
            field = field,
            ascending = field.defaultOrderAscending
        )
        changeSortType(sortType)
    }

    private fun showItemDetail(type: DetailType, library: Library) {
        ScreenArguments.itemDetailsArgs = ItemDetailsArgs(type, library = library, childKey = null)
        triggerEffect(AllItemsViewEffect.ShowItemDetailEffect)
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val args = ScreenArguments.allItemsArgs
        initViewState(args)

        loadInitialState()
    }

    private var coroutineScope = CoroutineScope(dispatchers.default)
    private fun setupFileObservers() {
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                val (progress, remainingCount, totalCount) = fileDownloader.batchData
                val batchData = progress?.let { ItemsState.DownloadBatchData.init(progress = progress, remaining = remainingCount, total = totalCount)}
                process(update = update, batchData = batchData)

                if (update.kind is AttachmentDownloader.Update.Kind.progress) {
                    return@onEach
                }
                if (viewState.attachmentToOpen != update.key) {
                    return@onEach
                }
                attachmentOpened(update.key)
                when (update.kind) {
                    AttachmentDownloader.Update.Kind.ready -> {
                        showAttachment(key = update.key, parentKey = update.parentKey, libraryId = update.libraryId)
                    }
                    is AttachmentDownloader.Update.Kind.failed -> {
                        //TODO implement when unzipping is supported
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun process(
        update: AttachmentDownloader.Update,
        batchData: ItemsState.DownloadBatchData?
    ) {
        val updateKey = update.parentKey ?: update.key
        val accessory = itemAccessories[updateKey] ?: return
        val attachment = accessory.attachmentGet ?: return
        viewModelScope.launch {
            when (update.kind) {
                AttachmentDownloader.Update.Kind.ready -> {
                    val updatedAttachment =
                        attachment.changed(location = Attachment.FileLocation.local)
                            ?: return@launch

                    itemAccessories[updateKey] = ItemAccessory.attachment(updatedAttachment)

                    updateState {
                        copy(
                            updateItemKey = updateKey
                        )
                    }
                    if (viewState.downloadBatchData != batchData) {
                        updateState {
                            copy(downloadBatchData = batchData)
                        }
                    }
                }
                AttachmentDownloader.Update.Kind.cancelled, is AttachmentDownloader.Update.Kind.failed, is AttachmentDownloader.Update.Kind.progress -> {
                    updateState {
                        copy(updateItemKey = updateKey)
                    }
                    if (viewState.downloadBatchData != batchData) {
                        updateState {
                            copy(downloadBatchData = batchData)
                        }
                    }

                }
            }
            val cellModel = viewState.itemKeyToItemCellModelMap[updateKey]!!
            cellModel.updateAccessory(cellAccessory(itemAccessories[updateKey]))
            triggerEffect(AllItemsViewEffect.ScreenRefresh)
        }
    }

    fun attachment(key: String, parentKey: String?, libraryId: LibraryIdentifier): Pair<Attachment, Library>? {
        val accessory = itemAccessories[parentKey ?: key] ?: return null
        val attachment = accessory.attachmentGet ?: return null
        return attachment to viewState.library
    }

    private suspend fun showAttachment(key: String, parentKey: String?, libraryId: LibraryIdentifier) {
        val attachmentResult = attachment(key = key, parentKey = parentKey, libraryId = libraryId)
        if (attachmentResult == null) {
            return
        }
        val (attachment, library) = attachmentResult
        viewModelScope.launch {
            show(attachment = attachment, library = library)
        }
    }

    private suspend fun show(attachment: Attachment, library: Library) {
        val attachmentType = attachment.type
        when (attachmentType) {
            is Attachment.Kind.url -> {
                showUrl(attachmentType.url)
            }
            is Attachment.Kind.file -> {
                val filename = attachmentType.filename
                val contentType = attachmentType.contentType
                val file = fileStore.attachmentFile(
                    libraryId = library.identifier,
                    key = attachment.key,
                    filename = filename,
                    contentType = contentType
                )
                when (contentType) {
                    "application/pdf" -> {
                        showPdf(file = file)
                    }
                    "text/html", "text/plain" -> {
                        openFile(file = file, mime = contentType)
                    }
                    else -> {
                        if (contentType.contains("image")) {
                            showImageFile(file)
                        } else if (contentType.contains("video")) {
                            showVideoFile(file)
                        }
                    }
                }
            }
        }
    }

    private fun showVideoFile(file: File) {
        ScreenArguments.videoPlayerArgs = VideoPlayerArgs(Uri.fromFile(file))
        triggerEffect(AllItemsViewEffect.ShowVideoPlayer)
    }

    private fun showImageFile(file: File) {
        ScreenArguments.imageViewerArgs = ImageViewerArgs(Uri.fromFile(file), file.name)
        triggerEffect(AllItemsViewEffect.ShowImageViewer)
    }

    private fun showPdf(file: File) {
        triggerEffect(AllItemsViewEffect.ShowPdf(file))
    }

    private fun openFile(file: File, mime: String) {
        triggerEffect(AllItemsViewEffect.OpenFile(file, mime))
    }

    private fun attachmentOpened(key: String) {
        if (viewState.attachmentToOpen != key) {
            return
        }
        viewModelScope.launch {
            updateState {
                copy(attachmentToOpen = null)
            }
        }
    }

    private fun initViewState(args: AllItemsArgs) {
        updateState {
            copy(
                collection = args.collection,
                library = args.library,
                searchTerm = args.searchTerm,
                sortType = args.sortType,
                error = args.error
            )
        }
    }

    private fun processUpdate(
        items: RealmResults<RItem>,
        deletions: IntArray,
        insertions: IntArray,
        modifications: IntArray
    ) {
        if (viewState.isEditing) {
            val mutableSelectedItems = viewState.selectedItems.toMutableSet()
            deletions.sorted().reversed().forEach { idx ->
                val key = this.keys.removeAt(idx)
                mutableSelectedItems.remove(key)
            }
            updateState {
                copy(selectedItems = mutableSelectedItems)
            }
        }
        coroutineScope.launch {
            modifications.forEach { idx ->
                val item = items[idx]
                val itemAccessory = accessory(item!!)
                if (itemAccessory != null) {
                    this@AllItemsViewModel.itemAccessories.put(item.key, itemAccessory)
                }
                generateCellModelForRItem(item)
            }
            insertions.forEach { idx ->
                val item = items[idx]
                if (viewState.isEditing) {
                    this@AllItemsViewModel.keys.add(element = item!!.key, index = idx)
                }

                val itemAccessory = accessory(item!!)
                if (itemAccessory != null) {
                    this@AllItemsViewModel.itemAccessories.put(item.key, itemAccessory)
                }
                generateCellModelForRItem(item)
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

    private fun loadInitialState() {
        onSearchStateFlow
            .debounce(150)
            .map { text ->
                search(if (text.isEmpty()) null else text, ignoreOriginal = false)
            }
            .launchIn(viewModelScope)

        val sortType = defaults.getItemsSortType()
        val request = ReadItemsDbRequest(
            collectionId = viewState.collection.identifier,
            libraryId = viewState.library.identifier,
            defaults = defaults
        )
        val sortDescriptors = sortType.descriptors
        val results =
            dbWrapper.realmDbStorage.perform(request = request).sort(sortDescriptors.first, sortDescriptors.second)
        val resultsFrozen = results.freeze()
        this@AllItemsViewModel.results = results
        updateState {
            copy(
                sortType = sortType,
                error = if (results == null) ItemsError.dataLoading else null,
                snapshot = resultsFrozen,
                lce = LCE2.Content,
            )
        }
        processRealmResults(resultsFrozen)

        setupFileObservers()

        val term = viewState.searchTerm
        if (term != null && !term.isEmpty()) {
            initialSearch(term)
        }

        startObserving(results)
    }

    private fun initialSearch(text: String) {
        search(if(text.isEmpty()) null else text, ignoreOriginal = true)
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    private fun search(text: String?, ignoreOriginal: Boolean) {
        val results = results(
            searchText = text,
            filters = this.filters,
            collectionId = viewState.collection.identifier,
            sortType = viewState.sortType,
            libraryId = viewState.library.identifier
        ) ?: return

        updateState {
            copy(searchTerm = text)
        }
        updateResults(results)
    }

    private fun updateResults(results: RealmResults<RItem>) {
        this.results?.removeAllChangeListeners()
        this.results = results
        startObserving(results)
        updateState {
            copy(snapshot = results.freeze())
        }
    }

    private fun processRealmResults(realmResults: RealmResults<RItem>?) {
        if (realmResults == null) {
            return
        }
        realmResults.map { rItem ->
            generateCellModelForRItem(rItem)
        }
    }

    private fun generateCellModelForRItem(rItem: RItem) {
        coroutineScope.launch {
            cacheItemAccessory(item = rItem)

            val accessory = itemAccessories[rItem.key]
            val typeName =
                schemaController.localizedItemType(itemType = rItem.rawType) ?: rItem.rawType

            val cellModel = ItemCellModel.init(
                item = rItem,
                accessory = cellAccessory(accessory),
                typeName = typeName
            )
            viewModelScope.launch {
                val m = viewState.itemKeyToItemCellModelMap.toMutableMap()
                m[rItem.key] = cellModel
                updateState {
                    copy(itemKeyToItemCellModelMap = m)
                }
            }
        }
    }

    private fun startObserving(results: RealmResults<RItem>) {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> {items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    val itemsFrozen = items.freeze()
                    updateState {
                        copy(lce = LCE2.Content, snapshot = itemsFrozen)
                    }
                    processRealmResults(itemsFrozen)

                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    val correctedModifications = Database.correctedModifications(modifications = modifications, insertions = insertions, deletions = deletions)
                    val itemsFrozen = items.freeze()
                    updateState {
                        copy(lce = LCE2.Content, snapshot = itemsFrozen)
                    }
                    processUpdate(items = itemsFrozen, deletions = deletions, insertions = insertions, modifications = correctedModifications)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "ItemsViewController: could not load results")
                    updateState {
                        copy(error = ItemsError.dataLoading)
                    }
                    updateState {
                        copy(lce = LCE2.LoadError {})
                    }
                }
            }
        })

    }

    private fun storeIfNeeded(libraryId: LibraryIdentifier, collectionId: CollectionIdentifier? = null): CollectionIdentifier {
        if (defaults.getSelectedLibrary() == libraryId) {
            if (collectionId != null) {
                fileStore.setSelectedCollectionId(collectionId)
                return collectionId
            }
            return fileStore.getSelectedCollectionId()
        }

        val collectionId = collectionId ?: CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
        defaults.setSelectedLibrary(libraryId)
        fileStore.setSelectedCollectionId(collectionId)
        return collectionId

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

    fun onAddManually() {
        ScreenArguments.singlePickerArgs = SinglePickerArgs(
            singlePickerState = SinglePickerStateCreator.create(
                selected = "",
                schemaController
            ),
            showSaveButton = false,
            callPoint = SinglePickerResult.CallPoint.AllItemsShowItem
        )
        triggerEffect(ShowItemTypePickerEffect)

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

    private fun showItemDetail(item: RItem) {
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
                ScreenArguments.itemDetailsArgs = ItemDetailsArgs(
                    DetailType.preview(key = item.key),
                    library = viewState.library,
                    childKey = null
                )
                triggerEffect(AllItemsViewEffect.ShowItemDetailEffect)
            }
        }
    }

    private suspend fun saveNote(text: String, tags: List<Tag>, key: String) = withContext(dispatchers.io) {
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

    private fun cacheItemAccessory(item: RItem) {
        if (itemAccessories[item.key] != null) {
            return
        }
        val accessory = accessory(item) ?: return

        itemAccessories.put(item.key, accessory)
    }

    private fun cellAccessory(accessory: ItemAccessory?): ItemCellModel.Accessory? {
        when (accessory) {
            is ItemAccessory.attachment -> {
                val attachment = accessory.attachment
                val (progress, error) = this.fileDownloader.data(
                    key = attachment.key,
                    libraryId = attachment.libraryId
                )
                return ItemCellModel.Accessory.attachment(
                    State.stateFrom(
                        type = attachment.type,
                        progress = progress,
                        error = error
                    )
                )
            }
            is ItemAccessory.doi ->
                return ItemCellModel.Accessory.doi
            is ItemAccessory.url ->
                return ItemCellModel.Accessory.url
            null -> return null
        }
    }

    private fun selectItem(key: String) {
        updateState {
            copy(selectedItems = selectedItems + key)
        }
    }
    private fun deselectItem(key: String) {
        updateState {
            copy(selectedItems = selectedItems - key)
        }
    }

    private fun showMetadata(item: RItem) {
        showItemDetail(item)
//        resetActiveSearch() //TODO implement
    }

    fun onItemTapped(item: RItem) {
        if (viewState.isEditing) {
            if (viewState.selectedItems.contains(item.key)) {
                deselectItem(item.key)
            } else{
                selectItem(item.key)
            }
            return
        }

        val accessory = itemAccessories[item.key]
        if (accessory == null) {
            showMetadata(item)
            return
        }

        viewModelScope.launch {
            when (accessory) {
                is ItemAccessory.attachment -> {
                    val parentKey = if (item.key == accessory.attachment.key) null else item.key
                    open(attachment = accessory.attachment, parentKey = parentKey)
                }
                is ItemAccessory.doi -> showDoi(accessory.doi)
                is ItemAccessory.url -> showUrl(url = accessory.url)
            }
        }
    }

    fun onAccessoryTapped(item: RItem) {
        showMetadata(item)
    }

    private fun showDoi(doi: String) {
        val url = "https://doi.org/$doi"
        triggerEffect(AllItemsViewEffect.OpenWebpage(Uri.parse(url)))
    }

    private suspend fun showUrl(url: String) {
        val uri = Uri.parse(url)
        if (uri.scheme != null && uri.scheme != "http" && uri.scheme != "https") {
            val mimeType = getMimeTypeUseCase.execute(url)!!
            triggerEffect(AllItemsViewEffect.OpenFile(uri.toFile(), mimeType))
        } else {
            triggerEffect(AllItemsViewEffect.OpenWebpage(uri))
        }
    }

    fun open(attachment: Attachment, parentKey: String?) {
        val progress =
            this.fileDownloader.data(key = attachment.key, libraryId = attachment.libraryId).first
        if (progress != null) {
            if (viewState.attachmentToOpen == attachment.key) {
                updateState {
                    copy(attachmentToOpen = null)
                }
            }
            this.fileDownloader.cancel(key = attachment.key, libraryId = attachment.libraryId)
        } else {
            updateState {
                copy(attachmentToOpen = attachment.key)
            }
            this.fileDownloader.downloadIfNeeded(attachment = attachment, parentKey = parentKey)
        }
    }

    fun showSortPicker() {
        ScreenArguments.sortPickerArgs = SortPickerArgs(
            sortType = viewState.sortType
        )
        triggerEffect(AllItemsViewEffect.ShowSortPickerEffect)
    }

    private fun setSortOrder(ascending: Boolean) {
        val sortType = viewState.sortType.copy(ascending = ascending)
        changeSortType(sortType)
    }

    private fun changeSortType(sortType: ItemsSortType) {
        val results = results(
            searchText = viewState.searchTerm,
            filters = this.filters,
            collectionId = viewState.collection.identifier,
            sortType = sortType,
            libraryId = viewState.library.identifier
        ) ?: return
        updateState {
            copy(sortType = sortType)
        }
        updateResults(results)
        defaults.setItemsSortType(sortType)
    }

    private fun results(
        searchText: String?,
        filters: List<ItemsState.Filter>,
        collectionId: CollectionIdentifier,
        sortType: ItemsSortType,
        libraryId: LibraryIdentifier
    ): RealmResults<RItem>? {
        val request = ReadItemsDbRequest(
            collectionId = collectionId,
            libraryId = libraryId,
            defaults = defaults
        )
        var results = dbWrapper.realmDbStorage.perform(request = request)
        val text = searchText
        if (text != null && (!text.isEmpty())) {
            results = results.where().itemSearch(listOf(text)).findAll()
        }

        if (!filters.isEmpty()) {
            for (filter in filters) {
                when (filter) {
                    ItemsState.Filter.downloadedFiles -> {
                        results = results
                            .where()
                            .equalTo("fileDownloaded", true)
                            .or()
                            .equalTo("children.fileDownloaded", true)
                            .findAll()
                    }
                }
            }
        }
        val sort = sortType.descriptors
        return results.sort(sort.first, sort.second)
    }

    private fun enable(filter: ItemsState.Filter) {
        if (this.filters.contains(filter)) {
            return
        }
        filters.add(filter)
        filter()
    }

    private fun disable(filter: ItemsState.Filter) {
        val index = this.filters.indexOf(filter)
        if (index == -1) {
            return
        }
        this.filters.removeAt(index)
        filter()
    }

    private fun filter() {
        val results = results(
            viewState.searchTerm,
            filters = this.filters,
            collectionId = viewState.collection.identifier,
            sortType = viewState.sortType,
            libraryId = viewState.library.identifier
        ) ?: return

        updateResults(results)
    }

    fun showFilters() {
        ScreenArguments.filterArgs = FilterArgs(
            filters = this.filters
        )
        triggerEffect(AllItemsViewEffect.ShowFilterEffect)
    }

    private fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        viewModelScope.launch {
            when (longPressOptionItem) {
                is LongPressOptionItem.MoveToTrashItem -> {
                    trashItems(setOf(longPressOptionItem.item.key))
                }
                is LongPressOptionItem.Download -> {
                    downloadAttachments(setOf(longPressOptionItem.item.key))
                }
                is LongPressOptionItem.RemoveDownload -> {
                    removeDownloads(setOf(longPressOptionItem.item.key))
                }
                is LongPressOptionItem.Duplicate -> {
                    loadItemForDuplication(longPressOptionItem.item.key)
                }
                is LongPressOptionItem.CreateParentItem -> {
                    createParent(longPressOptionItem.item)
                }
                is LongPressOptionItem.TrashDelete -> {
                    showDeleteItemsConfirmation(
                        setOf(longPressOptionItem.item.key)
                    )
                }
                is LongPressOptionItem.TrashRestore -> {
                    set(trashed = false, setOf(longPressOptionItem.item.key))
                }
            }
        }
    }

    private fun createParent(item: RItem) {
        val key = item.key
        val accessory = this.itemAccessories[key] ?: return
        val attachment = (accessory as? ItemAccessory.attachment)?.attachment ?:return
        var collectionKey: String? = null
        when(viewState.collection.identifier) {
            is CollectionIdentifier.collection ->
            collectionKey = viewState.collection.identifier.keyGet
            else -> {
                //no-op
            }
        }
        showItemDetail(
            DetailType.creation(
                type = ItemTypes.document,
                child = attachment,
                collectionKey = collectionKey
            ), library = viewState.library
        )
    }

    fun onItemLongTapped(item: RItem) {
        if (viewState.collection.identifier.isTrash) {
            EventBus.getDefault().post(
                ShowDashboardLongPressBottomSheet(
                    title = item.displayTitle,
                    longPressOptionItems = listOf(
                        LongPressOptionItem.TrashRestore(item),
                        LongPressOptionItem.TrashDelete(item)
                    )
                )
            )
            return
        }

        val actions = mutableListOf<LongPressOptionItem>()

        if (item.rawType == ItemTypes.attachment && item.parent == null) {
            actions.add(LongPressOptionItem.CreateParentItem(item))
        }

        val accessory = this.itemAccessories[item.key]
        if (accessory != null) {
            val location = accessory.attachmentGet?.location
            if (location != null) {
                when(location) {
                    Attachment.FileLocation.local -> {
                        actions.add(LongPressOptionItem.RemoveDownload(item))
                    }
                    Attachment.FileLocation.remote -> {
                        actions.add(LongPressOptionItem.Download(item))
                    }
                    Attachment.FileLocation.localAndChangedRemotely -> {
                        actions.add(LongPressOptionItem.Download(item))
                        actions.add(LongPressOptionItem.RemoveDownload(item))
                    }
                    Attachment.FileLocation.remoteMissing -> {
                        //no-op
                    }
                }
            }
        }

        if (item.rawType != ItemTypes.note && item.rawType != ItemTypes.attachment) {
            actions.add(LongPressOptionItem.Duplicate(item))
        }

        actions.add(LongPressOptionItem.MoveToTrashItem(item))

        EventBus.getDefault().post(
            ShowDashboardLongPressBottomSheet(
                title = item.displayTitle,
                longPressOptionItems = actions
            )
        )
    }

    fun delete(keys: Set<String>) {
        viewModelScope.launch {
            val result = perform(
                dbWrapper = dbWrapper,
                request = MarkObjectsAsDeletedDbRequest(
                    clazz = RItem::class,
                    keys = keys.toList(),
                    libraryId = viewState.library.identifier
                )
            ).ifFailure {
                Timber.e(it, "AllItemsViewModel: can't delete items")
                updateState {
                    copy(error = ItemsError.deletion)
                }
                return@launch
            }
        }

    }

    private suspend fun trashItems(keys: Set<String>) {
        set(trashed = true, keys = keys)
    }

    private suspend fun set(trashed: Boolean, keys: Set<String>) {
        val request = MarkItemsAsTrashedDbRequest(
            keys = keys.toList(),
            libraryId = viewState.library.identifier,
            trashed = trashed
        )
        perform(dbWrapper = dbWrapper, request = request).ifFailure { error ->
            Timber.e(error, "ItemsStore: can't trash items")
            updateState {
                copy(
                    error = ItemsError.deletion,
                )
            }
            return
        }
    }

    private fun downloadAttachments(keys: Set<String>) {
        for (key in keys) {
            val progress =
                this.fileDownloader.data(key, libraryId = viewState.library.identifier).first
            if (progress != null) {
                return
            }
            val attachment = itemAccessories[key]?.attachmentGet ?: return
            this.fileDownloader.downloadIfNeeded(
                attachment = attachment,
                parentKey = if (attachment.key == key) null else key
            )
        }
    }
    private fun removeDownloads(ids: Set<String>) {
        this.fileCleanupController.delete(
            AttachmentFileCleanupController.DeletionType.allForItems(
                keys = ids,
                libraryId = viewState.library.identifier
            ), completed = null
        )
    }

    private fun loadItemForDuplication(key: String) {
        val request = ReadItemDbRequest(libraryId = viewState.library.identifier, key = key)

        try {
            val item = dbWrapper.realmDbStorage.perform(request = request)
            stopEditing()
            showItemDetail(DetailType.duplication(itemKey = item.key, collectionKey = viewState.collection.identifier.keyGet), library = viewState.library)
        } catch (error: Exception) {
            Timber.e(error, "ItemsActionHandler: could not read item")
            updateState {
                copy(error = ItemsError.duplicationLoading)
            }
        }
    }

    private fun startEditing() {
        var keys = emptyList<String>()
        val results = this.results
        if (results != null) {
            keys = results.map { it.key }
        }
        this.keys = keys.toMutableList()
        updateState {
            copy(
                isEditing = true,
            )
        }
    }

    private fun stopEditing() {
        this.keys.clear()
        updateState {
            copy(
                isEditing = false,
                selectedItems = emptySet()
            )
        }
    }

    fun onSelect() {
        startEditing()
    }

    fun onDone() {
        stopEditing()
    }

    fun toggleSelectionState() {
        if (viewState.selectedItems.size != this.results?.size) {
            updateState {
                copy(selectedItems = (this@AllItemsViewModel.results?.map { it.key }
                    ?: emptyList()).toSet())
            }
        } else{
            updateState {
                copy(selectedItems = emptySet())
            }
        }
    }

    fun onDuplicate() {
        val key = viewState.selectedItems.first()
        loadItemForDuplication(key)

    }

    fun onTrash() {
        viewModelScope.launch {
            trashItems(viewState.selectedItems)
        }
    }

    fun onRestore() {
        viewModelScope.launch {
            set(trashed = false, keys = viewState.selectedItems)
        }
    }

    fun onDelete() {
        showDeleteItemsConfirmation(viewState.selectedItems)
    }

    fun onEmptyTrash() {
        updateState {
            copy(
                error = ItemsError.deleteConfirmationForEmptyTrash,
            )
        }
    }

    private fun showDeleteItemsConfirmation(itemsKeys: Set<String>) {
        updateState {
            copy(
                error = ItemsError.deleteConfirmationForItems(itemsKeys),
            )
        }
    }

    fun navigateToCollections() {
        ScreenArguments.collectionsArgs = CollectionsArgs(libraryId = defaults.getSelectedLibrary(), fileStore.getSelectedCollectionId())
        triggerEffect(AllItemsViewEffect.ShowCollectionsEffect)
    }

    fun onDismissDialog() {
        updateState {
            copy(
                error = null,
            )
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val result = perform(
                dbWrapper = dbWrapper,
                request = EmptyTrashDbRequest(libraryId = viewState.library.identifier)
            ).ifFailure {
                Timber.e(it, "AllItemsViewModel: can't empty trash")
                return@launch
            }
        }
    }

    fun startSync() {
        if (viewState.isRefreshing) {
            return
        }
        updateState {
            copy(isRefreshing = true)
        }
        this.syncScheduler.request(
            type = SyncType.ignoreIndividualDelays, libraries = LibrarySyncType.specific(
                listOf(viewState.library.identifier)
            )
        )
        viewModelScope.launch {
            //TODO monitor sync progress and don't sync when sync is in progress
            delay(3000)
            updateState {
                copy(isRefreshing = false)
            }
        }
    }
}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val snapshot: RealmResults<RItem>? = null,
    val itemKeyToItemCellModelMap: Map<String, ItemCellModel> = emptyMap(),
    val collection: Collection = Collection(
        identifier = CollectionIdentifier.collection(""),
        name = "",
        itemCount = 0
    ),
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val selectedItems: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val error: ItemsError? = null,
    val shouldShowAddBottomSheet: Boolean = false,
    val searchTerm: String? = null,
    val sortType: ItemsSortType = ItemsSortType.default,
    val itemKeyToDuplicate: String? = null,
    val updateItemKey: String? = null,
    val processingBibliography: Boolean = false,
    val bibliographyError: Throwable? = null,
    val attachmentToOpen: String? = null,
    val downloadBatchData: ItemsState.DownloadBatchData? = null,
    val isRefreshing: Boolean = false
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
    object ShowCollectionsEffect: AllItemsViewEffect()
    object ShowItemDetailEffect: AllItemsViewEffect()
    object ShowAddOrEditNoteEffect: AllItemsViewEffect()
    object ShowItemTypePickerEffect : AllItemsViewEffect()
    object ShowSortPickerEffect : AllItemsViewEffect()
    object ShowFilterEffect : AllItemsViewEffect()
    data class OpenWebpage(val uri: Uri) : AllItemsViewEffect()
    data class OpenFile(val file: File, val mimeType: String) : AllItemsViewEffect()
    object ShowVideoPlayer : AllItemsViewEffect()
    object ShowImageViewer : AllItemsViewEffect()
    data class ShowPdf(val file: File) : AllItemsViewEffect()
    object ScreenRefresh : AllItemsViewEffect()
}
