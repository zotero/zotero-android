package org.zotero.android.screens.allitems

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.androidx.content.copyHtmlToClipboard
import org.zotero.android.androidx.content.copyPlainTextToClipboard
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.citation.CitationController
import org.zotero.android.citation.CitationController.Format
import org.zotero.android.database.DbError
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.AssignItemsToCollectionsDbRequest
import org.zotero.android.database.requests.CreateAttachmentsDbRequest
import org.zotero.android.database.requests.CreateNoteDbRequest
import org.zotero.android.database.requests.DeleteItemsFromCollectionDbRequest
import org.zotero.android.database.requests.EditNoteDbRequest
import org.zotero.android.database.requests.EmptyTrashDbRequest
import org.zotero.android.database.requests.MarkItemsAsTrashedDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.helpers.MediaSelectionResult
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.pdf.data.PdfReaderArgs
import org.zotero.android.screens.addbyidentifier.data.AddByIdentifierPickerArgs
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.allitems.AllItemsViewEffect.ShowAddByIdentifierEffect
import org.zotero.android.screens.allitems.AllItemsViewEffect.ShowItemTypePickerEffect
import org.zotero.android.screens.allitems.AllItemsViewEffect.ShowScanBarcode
import org.zotero.android.screens.allitems.data.ItemAccessory
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.allitems.processor.AllItemsProcessor
import org.zotero.android.screens.allitems.processor.AllItemsProcessorInterface
import org.zotero.android.screens.citation.singlecitation.data.SingleCitationArgs
import org.zotero.android.screens.citation.singlecitation.locatorsList
import org.zotero.android.screens.citbibexport.data.CitBibExportArgs
import org.zotero.android.screens.collectionpicker.data.CollectionPickerArgs
import org.zotero.android.screens.collectionpicker.data.CollectionPickerMode
import org.zotero.android.screens.collectionpicker.data.CollectionPickerMultiResult
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.filter.data.FilterArgs
import org.zotero.android.screens.filter.data.FilterReloadEvent
import org.zotero.android.screens.filter.data.FilterResult
import org.zotero.android.screens.filter.data.UpdateFiltersEvent
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.screens.itemdetails.data.ItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataArgs
import org.zotero.android.screens.sortpicker.data.SortPickerArgs
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerStateCreator
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import timber.log.Timber
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
internal class AllItemsViewModel @Inject constructor(
    private val context: Context,
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    private val schemaController: SchemaController,
    private val syncScheduler: SyncScheduler,
    private val allItemsProcessor: AllItemsProcessor,
    private val dispatchers: Dispatchers,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val defaults: Defaults,
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()),
    AllItemsProcessorInterface {

    @Inject
    lateinit var citationControllerProvider: Provider<CitationController>

    private var collection: Collection = Collection(
        identifier = CollectionIdentifier.collection(""),
        name = "",
        itemCount = 0
    )

    private var library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    )

    private var isTablet: Boolean = false

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
        println()
        if (saveNoteAction.isFromDashboard) {
            viewModelScope.launch {
                saveNote(saveNoteAction.text, saveNoteAction.tags, saveNoteAction.key)
            }
        }
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
            is FilterResult.tagSelectionDidChange -> {
                tagSelectionDidChange(filterResult.selectedTags)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: CollectionPickerMultiResult) {
        add(itemKeys = preSelectedItemKeysToAddToCollection, collectionKeys = result.keys)
    }

    override fun showItemDetailWithDelay(creation: DetailType.creation) {
        viewModelScope.launch {
            delay(800)
            showItemDetail(
                type = creation, library = this@AllItemsViewModel.library
            )
        }
    }

    private fun showItemDetail(type: DetailType, library: Library) {
        val args = ItemDetailsArgs(type, library = library, childKey = null)
        val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(args)
        triggerEffect(AllItemsViewEffect.ShowItemDetailEffect(encodedArgs))
    }

    fun init(isTablet: Boolean) = initOnce {
        viewModelScope.launch {
            this@AllItemsViewModel.isTablet = isTablet
            EventBus.getDefault().register(this@AllItemsViewModel)
            val args = ScreenArguments.allItemsArgs
            this@AllItemsViewModel.collection = args.collection
            this@AllItemsViewModel.library = args.library

            val searchTerm = args.searchTerm

            updateState {
                copy(
                    searchTerm = searchTerm,
                    error = args.error,
                    isCollectionTrash = this@AllItemsViewModel.collection.identifier.isTrash,
                    isCollectionACollection = this@AllItemsViewModel.collection.identifier.isCollection,
                    collectionName = this@AllItemsViewModel.collection.name
                )
            }

            allItemsProcessor.init(
                allItemsProcessorInterface = this@AllItemsViewModel,
                searchTerm = searchTerm
            )
        }


    }

    override fun show(attachment: Attachment, parentKey: String?, library: Library) {
        viewModelScope.launch {
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
                    )
                    when (contentType) {
                        "application/pdf" -> {
                            showPdf(
                                file = file,
                                key = attachment.key,
                                parentKey = parentKey,
                                library = library
                            )
                        }

                        "text/html", "text/plain" -> {
                            val url = file.toUri().toString()
                            val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                            triggerEffect(AllItemsViewEffect.ShowZoteroWebView(encodedUrl))
                        }
                        else -> {
                            if (contentType.contains("image")) {
                                showImageFile(file)
                            } else if (contentType.contains("video")) {
                                showVideoFile(file)
                            } else {
                                openFile(file, contentType)
                            }
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

    private fun showPdf(file: File, key: String, parentKey: String?, library: Library) {
        val uri = file.toUri()
        val pdfReaderArgs = PdfReaderArgs(
            key = key,
            parentKey = parentKey,
            library = library,
            page = null,
            preselectedAnnotationKey = null,
            uri = uri,
        )
        val params = navigationParamsMarshaller.encodeObjectToBase64(pdfReaderArgs, StandardCharsets.UTF_8)
        triggerEffect(AllItemsViewEffect.NavigateToPdfScreen(params))
    }

    private fun openFile(file: File, mime: String) {
        triggerEffect(AllItemsViewEffect.OpenFile(file, mime))
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        allItemsProcessor.onSearch(text = text)
    }



    override fun updateTagFilter() {
        EventBus.getDefault().post(
            FilterReloadEvent(
                filters = viewState.filters,
                collectionId = this.collection.identifier,
                libraryId = this.library.identifier,
            )
        )
    }

    override fun currentLibrary(): Library {
        return this.library
    }

    override fun currentCollection(): Collection {
        return this.collection
    }

    override fun currentSearchTerm(): String? {
        return viewState.searchTerm
    }

    override fun currentFilters(): List<ItemsFilter> {
        return viewState.filters
    }

    override fun isEditing(): Boolean {
        return viewState.isEditing
    }

    fun onAdd() {
        updateState {
            copy(
                shouldShowAddBottomSheet = true
            )
        }
    }

    fun onAddBottomSheetCollapse() {
        updateState {
            copy(
                shouldShowAddBottomSheet = false
            )
        }
    }

    override fun sendChangesToUi(
        updatedItemCellModels: SnapshotStateList<ItemCellModel>?,
        updatedDownloadingAccessories: SnapshotStateMap<String, ItemCellModel.Accessory?>?
    ) {
        viewModelScope.launch {
            val newItemCellModels = updatedItemCellModels ?: viewState.itemCellModels
            val maybeFirstItemAfterUpdate = newItemCellModels.firstOrNull()
            val wasTheFirstItemRecentlyAdded =
                maybeFirstItemAfterUpdate != null && !viewState.itemCellModels.any { it.key == maybeFirstItemAfterUpdate.key }
            updateState {
                copy(
                    itemCellModels = newItemCellModels,
                    accessoryBeingDownloaded = updatedDownloadingAccessories
                        ?: viewState.accessoryBeingDownloaded
                )
            }
            if (wasTheFirstItemRecentlyAdded) {
                triggerEffect(AllItemsViewEffect.MaybeScrollToTop)
            }
        }

    }

    override fun updateLCE(lce: LCE2) {
        updateState {
            copy(lce = lce)
        }
    }

    override fun showError(error: ItemsError) {
        updateState {
            copy(error = error)
        }
    }

    override fun onCleared() {
        allItemsProcessor.clear()
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private suspend fun addAttachments(urls: List<Uri>) {
        val libraryId = this.library.identifier
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
            val filename = original.name
            //I was able to reproduce crash with unrecognizable contentType only when file's extension was empty. If so we now treat such file as binary.
            val contentType =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(original.extension)
                    ?: "application/octet-stream"
            val file =
                fileStore.attachmentFileAsync(libraryId = libraryId, key = key, filename = filename)
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
        val identifier = this.collection.identifier
        collections = when(identifier) {
            is CollectionIdentifier.collection -> {
                setOf(identifier.key)
            }

            is CollectionIdentifier.search, is CollectionIdentifier.custom -> {
                emptySet()
            }
        }
        val type = schemaController.localizedItemType(ItemTypes.attachment) ?: ""
        val request = CreateAttachmentsDbRequest(attachments = attachments, parentKey = null, localizedType = type, collections = collections, fileStore = fileStore)

        val result = perform(dbWrapperMain, invalidateRealm = true, request = request).ifFailure {
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
        showNoteCreation(title = null, libraryId = this.library.identifier)
    }

    fun onAddManually(title: String) {
        ScreenArguments.singlePickerArgs = SinglePickerArgs(
            singlePickerState = SinglePickerStateCreator.create(
                selected = "",
                schemaController
            ),
            showSaveButton = false,
            title = title,
            callPoint = SinglePickerResult.CallPoint.AllItemsShowItem
        )
        triggerEffect(ShowItemTypePickerEffect)

    }

    private fun showNoteCreation(title: AddOrEditNoteArgs.TitleData?, libraryId: LibraryIdentifier) {
        val key = KeyGenerator.newKey()
        val args = AddOrEditNoteArgs(
            title = title,
            key = key,
            libraryId = libraryId,
            readOnly = false,
            isFromDashboard = true,
        )
        val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(
            data = args,
            charset = StandardCharsets.UTF_8
        )
        triggerEffect(AllItemsViewEffect.ShowAddOrEditNoteEffect(encodedArgs))
    }

    private fun showItemDetail(item: RItem) {
        when (item.rawType) {
            ItemTypes.note -> {
                val note = Note.init(item = item)
                if (note == null) {
                    return
                }
                val library = this.library
                val args = AddOrEditNoteArgs(
                    title = null,
                    libraryId = library.identifier,
                    readOnly = !library.metadataEditable,
                    key = note.key,
                    isFromDashboard = true
                )
                val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(
                    data = args,
                    charset = StandardCharsets.UTF_8
                )
                triggerEffect(AllItemsViewEffect.ShowAddOrEditNoteEffect(encodedArgs))
            }

            else -> {
                val args = ItemDetailsArgs(
                    DetailType.preview(key = item.key),
                    library = this.library,
                    childKey = null
                )
                val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(args)
                triggerEffect(AllItemsViewEffect.ShowItemDetailEffect(encodedArgs))
            }
        }
    }

    private suspend fun saveNote(text: String, tags: List<Tag>, key: String) = withContext(dispatchers.io) {
        val note = Note(key = key, text = text, tags = tags)
        val libraryId = this@AllItemsViewModel.library.identifier
        var collectionKey: String?

        val identifier = this@AllItemsViewModel.collection.identifier
        collectionKey = when (identifier) {
            is CollectionIdentifier.collection ->
                identifier.key

            is CollectionIdentifier.custom, is CollectionIdentifier.search ->
                null
        }

        try {
            dbWrapperMain.realmDbStorage.perform(
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
                dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
            } else {
                Timber.e(e)
            }
        }
    }

    private fun showMetadata(item: RItem) {
        showItemDetail(item)
//        resetActiveSearch() //TODO implement
    }

    fun onItemTapped(item: ItemCellModel) {
        if (viewState.isEditing) {
            addItemToSelection(item.key)
            return
        }

        val accessory = allItemsProcessor.getItemAccessoryByKey(item.key)
        if (accessory == null) {
            showMetadata(allItemsProcessor.getResultByKey(item.key)!!)
            return
        }

        viewModelScope.launch {
            when (accessory) {
                is ItemAccessory.attachment -> {
                    val parentKey = if (item.key == accessory.attachment.key) null else item.key
                    allItemsProcessor.open(attachment = accessory.attachment, parentKey = parentKey)
                }
                is ItemAccessory.doi -> showDoi(accessory.doi)
                is ItemAccessory.url -> showUrl(url = accessory.url)
            }
        }
    }

    private fun addItemToSelection(key: String) {
        val selectedKeys = viewState.selectedKeys!!
        val isCurrentlySelected = selectedKeys.contains(key)
        updateState {
            copy(
                selectedKeys = if (isCurrentlySelected) {
                    selectedKeys.remove(key)
                } else {
                    selectedKeys.add(key)
                }
            )
        }
    }

    fun onAccessoryTapped(key:String) {
        allItemsProcessor.getResultByKey(key)?.let {
            showMetadata(it)
        }
    }

    private fun showDoi(doi: String) {
        val url = "https://doi.org/$doi"
        triggerEffect(AllItemsViewEffect.OpenWebpage(url))
    }

    private suspend fun showUrl(url: String) {
        val uri = url.toUri()
        if (uri.scheme != null && uri.scheme != "http" && uri.scheme != "https") {
            val mimeType = getUriDetailsUseCase.getMimeType(url)!!
            triggerEffect(AllItemsViewEffect.OpenFile(uri.toFile(), mimeType))
        } else {
            triggerEffect(AllItemsViewEffect.OpenWebpage(url))
        }
    }

    fun showSortPicker() {
        ScreenArguments.sortPickerArgs = SortPickerArgs(
            sortType = allItemsProcessor.getSortType()
        )
        triggerEffect(AllItemsViewEffect.ShowSortPickerEffect)
    }


    private fun enable(filter: ItemsFilter) {
        if (viewState.filters.contains(filter)) {
            return
        }
        if (filter is ItemsFilter.tags) {
            updateState {
                copy(
                    filters = viewState.filters.filter { it !is ItemsFilter.tags }.toPersistentList()
                )
            }
        }
        updateState {
            copy(
                filters = (viewState.filters + filter).toPersistentList()
            )
        }
        EventBus.getDefault().post(UpdateFiltersEvent(viewState.filters))
        allItemsProcessor.filter(searchTerm = viewState.searchTerm, filters = viewState.filters)
    }

    private fun disable(filter: ItemsFilter) {
        val index = viewState.filters.indexOf(filter)
        if (index == -1) {
            return
        }
        updateState {
            copy(
                filters = (viewState.filters - filter).toPersistentList()
            )
        }
        EventBus.getDefault().post(UpdateFiltersEvent(viewState.filters))
        allItemsProcessor.filter(searchTerm = viewState.searchTerm, filters = viewState.filters)
    }

    fun showFilters() {
        if (isTablet) {
            onShowDownloadedFilesPopupClicked()
        } else {
            val args = createShowFilterArgs()
            val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(args)
            triggerEffect(AllItemsViewEffect.ShowPhoneFilterEffect(encodedArgs))
        }

    }

    fun onTrashRestore() = viewModelScope.launch {
        set(trashed = false, getSelectedKeys())
        stopEditing()
    }

    fun onTrashDelete() = viewModelScope.launch {
        showDeleteItemsConfirmation(itemsKeys = getSelectedKeys())
    }

    private var preSelectedItemKeysToAddToCollection: Set<String> = emptySet()

    private fun showCollectionPicker(selectedItemKeys: Set<String>) {
        preSelectedItemKeysToAddToCollection = selectedItemKeys
        ScreenArguments.collectionPickerArgs = CollectionPickerArgs(
            mode = CollectionPickerMode.multiple,
            libraryId = this.library.identifier,
            excludedKeys = emptySet(),
            selected = emptySet()
        )
        triggerEffect(AllItemsViewEffect.ShowCollectionPickerEffect)
    }

    private fun showRetrieveMetadataDialog(itemKey: String, libraryId: LibraryIdentifier) {
        val args = RetrieveMetadataArgs(
            itemKey = itemKey,
            libraryId = libraryId,
        )
        val params = navigationParamsMarshaller.encodeObjectToBase64(args)
        triggerEffect(AllItemsViewEffect.ShowRetrieveMetadataDialogEffect(params))
    }


    private fun createParent(item: RItem) {
        val key = item.key
        val accessory = allItemsProcessor.getItemAccessoryByKey(key)?: return
        val attachment = (accessory as? ItemAccessory.attachment)?.attachment ?:return
        var collectionKey: String? = null
        when(this.collection.identifier) {
            is CollectionIdentifier.collection ->
            collectionKey = this.collection.identifier.keyGet
            else -> {
                //no-op
            }
        }
        showItemDetail(
            DetailType.creation(
                type = ItemTypes.document,
                child = attachment,
                collectionKey = collectionKey
            ), library = this.library
        )
    }

    fun onItemLongTapped(key: String) {
        if (!isEditing()) {
            startEditing()
            addItemToSelection(key)
        }

    }

    fun delete(keys: Set<String>) {
        stopEditing()
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = MarkObjectsAsDeletedDbRequest(
                    clazz = RItem::class,
                    keys = keys.toList(),
                    libraryId = this@AllItemsViewModel.library.identifier
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
        stopEditing()
        set(trashed = true, keys = keys)
    }

    private suspend fun set(trashed: Boolean, keys: Set<String>) {
        val request = MarkItemsAsTrashedDbRequest(
            keys = keys.toList(),
            libraryId = this.library.identifier,
            trashed = trashed
        )
        perform(dbWrapper = dbWrapperMain, request = request).ifFailure { error ->
            Timber.e(error, "ItemsStore: can't trash items")
            updateState {
                copy(
                    error = ItemsError.deletion,
                )
            }
            return
        }
    }

    private fun loadItemForDuplication(key: String) {
        val request = ReadItemDbRequest(libraryId = this.library.identifier, key = key)

        try {
            val item = dbWrapperMain.realmDbStorage.perform(request = request)
            stopEditing()
            showItemDetail(DetailType.duplication(itemKey = item.key, collectionKey = this.collection.identifier.keyGet), library = this.library)
        } catch (error: Exception) {
            Timber.e(error, "ItemsActionHandler: could not read item")
            updateState {
                copy(error = ItemsError.duplicationLoading)
            }
        }
    }

    private fun startEditing() {
        updateState {
            copy(
                selectedKeys = persistentSetOf(),
            )
        }
    }

    private fun stopEditing() {
        updateState {
            copy(
                selectedKeys = null,
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
        if (!viewState.areAllSelected) {
            val allItemsKeys = viewState.itemCellModels.map { it.key }.toPersistentSet()
            updateState {
                copy(selectedKeys = allItemsKeys)
            }
        } else{
            updateState {
                copy(selectedKeys = persistentSetOf())
            }
        }
    }

    fun onDownloadSelectedAttachments() {
        allItemsProcessor.downloadSelectedAttachments(getSelectedKeys())
        stopEditing()
    }

    fun onRemoveSelectedAttachments() {
        allItemsProcessor.removeSelectedAttachments(getSelectedKeys())
        stopEditing()
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
        viewModelScope.launch {
            val collectionsArgs = CollectionsArgs(libraryId = fileStore.getSelectedLibraryAsync(), fileStore.getSelectedCollectionIdAsync())
            val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(collectionsArgs, StandardCharsets.UTF_8)
            triggerEffect(AllItemsViewEffect.ShowCollectionsEffect(encodedArgs))
        }
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
            perform(
                dbWrapper = dbWrapperMain,
                request = EmptyTrashDbRequest(libraryId = this@AllItemsViewModel.library.identifier)
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
            type = SyncKind.ignoreIndividualDelays, libraries = Libraries.specific(
                listOf(this.library.identifier)
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

    private fun tagSelectionDidChange(selected: Set<String>) {
        if (selected.isEmpty()) {
            val tags = viewState.tagsFilter
            if (tags != null) {
                disable(ItemsFilter.tags(tags))
            }
        } else {
            enable(ItemsFilter.tags(selected))
        }
    }

    fun onAddToCollection() {
       showCollectionPicker(getSelectedKeys())
    }

    fun showRemoveFromCollectionQuestion(itemsKeys: Set<String>) {
        updateState {
            copy(
                error = ItemsError.showRemoveFromCollectionQuestion(
                    itemsKeys
                )
            )
        }
    }

    fun deleteItemsFromCollection(keys: Set<String>) {
        val identifier = this.collection.identifier
        if (identifier !is CollectionIdentifier.collection) {
            return
        }

        val request = DeleteItemsFromCollectionDbRequest(
            collectionKey = identifier.key,
            itemKeys = keys,
            libraryId = this.library.identifier
        )
        stopEditing()
        viewModelScope.launch {
            perform(dbWrapperMain, request = request).ifFailure {
                Timber.e(it, "ItemsStore: can't delete items")
                updateState {
                    copy(error = ItemsError.deletionFromCollection)
                }
                return@launch
            }
        }
    }

    private fun add(itemKeys: Set<String>, collectionKeys: Set<String>) {
        stopEditing()
        val request = AssignItemsToCollectionsDbRequest(
            collectionKeys = collectionKeys,
            itemKeys = itemKeys,
            libraryId = this.library.identifier
        )

        viewModelScope.launch {
            perform(dbWrapperMain, request = request).ifFailure {
                Timber.e(it, "ItemsStore: can't assign collections to items")
                updateState {
                    copy(error = ItemsError.collectionAssignment)
                }
                return@launch
            }
        }
    }
    private fun getSelectedKeys(): Set<String> {
        return viewState.selectedKeys ?: emptySet()
    }

    fun onAddByIdentifier() {
        val addByIdentifierPickerArgs =
            AddByIdentifierPickerArgs(restoreLookupState = false)
        val params = navigationParamsMarshaller.encodeObjectToBase64(addByIdentifierPickerArgs)
        triggerEffect(ShowAddByIdentifierEffect(params))
    }

    fun dismissDownloadedFilesPopup() {
        updateState {
            copy(
                showDownloadedFilesPopup = false
            )
        }
    }

    private fun onShowDownloadedFilesPopupClicked() {
        updateState {
            copy(
                showDownloadedFilesPopup = true,
            )
        }
    }

    fun onDownloadedFilesTapped() {
        val newSelectedState = !viewState.isDownloadsFilterEnabled()
        if (newSelectedState) {
            enable(ItemsFilter.downloadedFiles)
        } else {
            disable(ItemsFilter.downloadedFiles)
        }
    }

    fun onScanBarcode() {
        triggerEffect(ShowScanBarcode)
    }

    private fun createShowFilterArgs(): FilterArgs {
        val selectedTags =
            viewState.filters.filterIsInstance<ItemsFilter.tags>().flatMap { it.tags }.toSet()
        val filterArgs = FilterArgs(
            filters = viewState.filters,
            collectionId = this.collection.identifier,
            libraryId = this.library.identifier,
            selectedTags = selectedTags
        )
        return filterArgs
    }

    private suspend fun showSingleCitation(selectedItemKeys: Set<String>) {
        stopEditing()
        if (selectedItemKeys.size == 1) {
            ScreenArguments.singleCitationArgs = SingleCitationArgs(
                itemIds = selectedItemKeys,
                libraryId = this.library.identifier,
            )
            triggerEffect(AllItemsViewEffect.ShowSingleCitationEffect)
        } else {
            updateState {
                copy(isGeneratingCitation = true)
            }
            copyMultipleCitations(selectedItemKeys)
            updateState {
                copy(isGeneratingCitation = false)
            }
        }

    }

    fun loadBibliography(selectedItemKeys: Set<String>) = viewModelScope.launch {
        updateState {
            copy(isGeneratingBibliography = true)
        }

        val styleId = defaults.getQuickCopyStyleId()
        val localeId = defaults.getQuickCopyCslLocaleId()
        val citationController = citationControllerProvider.get()
        val session = citationController.startSession(
            itemIds = selectedItemKeys,
            libraryId = this@AllItemsViewModel.library.identifier,
            styleId = styleId,
            localeId = localeId
        )
        val html = citationController.bibliography(session, format = Format.html)
        val resultPair: Pair<String, String?> = if (defaults.isQuickCopyAsHtml()) {
            html to null
        } else {
            html to citationController.bibliography(session = session, format = Format.text)
        }
        if (resultPair.second != null) {
            context.copyHtmlToClipboard(resultPair.first, text = resultPair.second!!)
        } else {
            context.copyPlainTextToClipboard(resultPair.first)
        }

        updateState {
            copy(isGeneratingBibliography = false)
        }

    }

    fun onCopyCitation() = viewModelScope.launch {
        showSingleCitation(getSelectedKeys())
    }

    fun onCopyBibliography() = viewModelScope.launch {
        loadBibliography(getSelectedKeys())
        stopEditing()
    }

    fun onShowRetrieveDialog() = viewModelScope.launch {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())!!
        showRetrieveMetadataDialog(
            itemKey = item.key,
            libraryId = item.libraryId!!
        )
    }

    fun onCreateDialog() {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())!!
        createParent(item)
    }

    fun onRemoveDownload() {
        allItemsProcessor.removeDownloads(getSelectedKeys())
        stopEditing()
    }

    fun onDownload() {
        allItemsProcessor.downloadAttachments(getSelectedKeys())
        stopEditing()
    }

    fun onRemoveFromCollection() {
        showRemoveFromCollectionQuestion(getSelectedKeys())
    }

    fun onDuplicate() {
        loadItemForDuplication(getSelectedKeys().first())
    }

    fun onMoveToTrash() = viewModelScope.launch {
        trashItems(getSelectedKeys())
    }

    fun shouldIncludeCopyCitationAndBibliographyButtons(): Boolean {
        val shouldNotInclude = getSelectedKeys().any { it ->
            val item = allItemsProcessor.getResultByKey(it)
            item != null && CitationController.invalidItemTypes.contains(item.rawType)
        }

        return !shouldNotInclude

    }

    fun shouldIncludeRetrieveMetadataButton(): Boolean {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())
        if (item == null) {
            return false
        }

        val attachment = allItemsProcessor.attachment(item.key, null)
        val contentType = (attachment?.first?.type as? Attachment.Kind.file)?.contentType
        return item.rawType == ItemTypes.attachment && item.parent == null && contentType == "application/pdf"
    }

    fun shouldIncludeCreateParentButton(): Boolean {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())
        return item != null && item.rawType == ItemTypes.attachment && item.parent == null
    }

    fun getAttachmentFileLocation(): Attachment.FileLocation? {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())
        if (item == null) {
            return null
        }

        val accessory = allItemsProcessor.getItemAccessoryByKey(item.key)
        if (accessory != null) {
            val location = accessory.attachmentGet?.location
            return location
        }
        return null
    }

    fun shouldIncludeRemoveFromCollectionButton(): Boolean {
        val identifier = this.collection.identifier
        val key = getSelectedKeys().first()
        val item = allItemsProcessor.getResultByKey(key)

        return (identifier is CollectionIdentifier.collection && item?.collections?.where()
            ?.key(identifier.key)?.findFirst() != null)
    }

    fun shouldIncludeDuplicateButton(): Boolean {
        val item = allItemsProcessor.getResultByKey(getSelectedKeys().first())
        if(item == null) {
            return false
        }
        return item.rawType != ItemTypes.note && item.rawType != ItemTypes.attachment
    }

    private suspend fun copyMultipleCitations(itemIds: Set<String>) = withContext(dispatchers.io) {
        val styleId = defaults.getQuickCopyStyleId()
        val localeId = defaults.getQuickCopyCslLocaleId()
        val exportAsHtml = defaults.isQuickCopyAsHtml()

        val citationController = citationControllerProvider.get()

        val session = citationController.startSession(
            itemIds = itemIds,
            libraryId = this@AllItemsViewModel.library.identifier,
            styleId = styleId,
            localeId = localeId
        )
        val locator: String = locatorsList[0]
        val locatorValue = ""

        val preview = citationController.citation(
            session = session,
            label = locator,
            locator = locatorValue,
            omitAuthor = false,
            format = Format.html,
            showInWebView = true
        )

        if (preview.isEmpty()) {
            return@withContext
        }

        if (exportAsHtml) {
            context.copyPlainTextToClipboard(preview)
            return@withContext
        }
        val text = citationController.citation(
            session = session,
            label = locator,
            locator = locatorValue,
            omitAuthor = false,
            format = Format.text,
            showInWebView = false
        )
        context.copyHtmlToClipboard(preview, text = text)
    }

    fun onShare() {
        ScreenArguments.citBibExportArgs = CitBibExportArgs(
            itemIds = getSelectedKeys(),
            libraryId = this.library.identifier
        )

        triggerEffect(AllItemsViewEffect.ShowCitationBibliographyExportEffect)
    }


}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Content,
    val snackbarMessage: SnackbarMessage? = null,
    val itemCellModels: SnapshotStateList<ItemCellModel> = mutableStateListOf(),
    val accessoryBeingDownloaded: SnapshotStateMap<String, ItemCellModel.Accessory?> = mutableStateMapOf(),
    val selectedKeys: PersistentSet<String>? = null,
    val error: ItemsError? = null,
    val shouldShowAddBottomSheet: Boolean = false,
    val searchTerm: String? = null,
    val isRefreshing: Boolean = false,
    val filters: PersistentList<ItemsFilter> = persistentListOf(),
    val isCollectionTrash: Boolean = false,
    val isCollectionACollection: Boolean = false,
    val collectionName: String = "",
    val showDownloadedFilesPopup: Boolean = false,
    val isGeneratingBibliography: Boolean = false,
    val isGeneratingCitation: Boolean = false,
) : ViewState {
    val tagsFilter: Set<String>?
        get() {
            val tagFilter = this.filters.firstOrNull { filter ->
                filter is ItemsFilter.tags
            }

            if (tagFilter == null || tagFilter !is ItemsFilter.tags) {
                return null
            }
            return tagFilter.tags
        }
    val isEditing : Boolean get() {
        return selectedKeys != null
    }
    fun isSelected(key: String): Boolean {
        return selectedKeys?.contains(key) == true
    }

    fun isAnythingSelected(): Boolean {
        return selectedKeys?.isNotEmpty() == true
    }
    val areAllSelected get(): Boolean {
        val size = itemCellModels.size
        return (selectedKeys?.size ?: 0) == size
    }

    fun getAccessoryForItem(itemKey: String): ItemCellModel.Accessory? {
        val beingDownloadedAccessory = accessoryBeingDownloaded[itemKey]
        if (beingDownloadedAccessory != null) {
            return beingDownloadedAccessory
        }
        return itemCellModels.firstOrNull { it.key == itemKey }?.accessory
    }

    fun isDownloadsFilterEnabled(): Boolean {
        return filters.any { it is ItemsFilter.downloadedFiles }
    }
}

internal sealed class AllItemsViewEffect : ViewEffect {
    data class ShowCollectionsEffect(val screenArgs: String): AllItemsViewEffect()
    data class ShowItemDetailEffect(val screenArgs: String): AllItemsViewEffect()
    data class ShowAddOrEditNoteEffect(val screenArgs: String): AllItemsViewEffect()
    object ShowItemTypePickerEffect : AllItemsViewEffect()
    data class ShowAddByIdentifierEffect(val params: String) : AllItemsViewEffect()
    object ShowSortPickerEffect : AllItemsViewEffect()
    object ShowCollectionPickerEffect: AllItemsViewEffect()
    data class ShowPhoneFilterEffect(val params: String) : AllItemsViewEffect()
    data class OpenWebpage(val url: String) : AllItemsViewEffect()
    data class OpenFile(val file: File, val mimeType: String) : AllItemsViewEffect()
    data class ShowZoteroWebView(val url: String): AllItemsViewEffect()
    object ShowVideoPlayer : AllItemsViewEffect()
    object ShowImageViewer : AllItemsViewEffect()
    data class NavigateToPdfScreen(val params: String) : AllItemsViewEffect()
    object ScreenRefresh : AllItemsViewEffect()
    object ShowScanBarcode : AllItemsViewEffect()
    data class ShowRetrieveMetadataDialogEffect(val params: String) : AllItemsViewEffect()
    object MaybeScrollToTop : AllItemsViewEffect()
    object ShowSingleCitationEffect: AllItemsViewEffect()
    object ShowCitationBibliographyExportEffect: AllItemsViewEffect()

}
