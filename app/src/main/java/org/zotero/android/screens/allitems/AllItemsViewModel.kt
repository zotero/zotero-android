package org.zotero.android.screens.allitems

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
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
import org.zotero.android.architecture.ifFailure
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.database.DbError
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.CreateAttachmentsDbRequest
import org.zotero.android.database.requests.CreateNoteDbRequest
import org.zotero.android.database.requests.EditNoteDbRequest
import org.zotero.android.database.requests.ReadItemsDbRequest
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
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.screens.itemdetails.data.ItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import org.zotero.android.uicomponents.attachmentprogress.State
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
    private val dispatcher: CoroutineDispatcher,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
    private val fileDownloader: AttachmentDownloader,
    private val getMimeTypeUseCase: GetMimeTypeUseCase,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    val schemaController: SchemaController,
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

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
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItems) {
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
        }
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
        setupFileObservers()
        val results = viewState.results
        if (results != null) {
            startObserving(results)
        }
    }

    private var coroutineScope = CoroutineScope(dispatcher)
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
        val accessory = viewState.itemAccessories[updateKey] ?: return
        val attachment = accessory.attachmentGet ?: return
        viewModelScope.launch {
            when (update.kind) {
                AttachmentDownloader.Update.Kind.ready -> {
                    val updatedAttachment =
                        attachment.changed(location = Attachment.FileLocation.local)
                            ?: return@launch

                    val updatedItemAccessories = viewState.itemAccessories.toMutableMap()
                    updatedItemAccessories[updateKey] = ItemAccessory.attachment(updatedAttachment)

                    updateState {
                        copy(
                            itemAccessories = updatedItemAccessories,
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
            triggerEffect(AllItemsViewEffect.ScreenRefresh)

        }
    }

    fun attachment(key: String, parentKey: String?, libraryId: LibraryIdentifier): Pair<Attachment, Library>? {
        val accessory = viewState.itemAccessories[parentKey ?: key] ?: return null
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
            deletions.sorted().reversed().forEach { idx ->
               val mutableKeys = viewState.keys.toMutableList()
                val key = mutableKeys.removeAt(idx)
                updateState {
                    copy(keys = mutableKeys)
                }


                val mutableSelectedItems = viewState.selectedItems.toMutableSet()
                mutableSelectedItems.remove(key)
                updateState {
                    copy(selectedItems = mutableSelectedItems)
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

    private fun loadInitialState() {
        val sortType = defaults.getItemsSortType()
        val request = ReadItemsDbRequest(collectionId = viewState.collection.identifier, libraryId = viewState.library.identifier, defaults = defaults)
        val results = dbWrapper.realmDbStorage.perform(request = request)//TODO sort by descriptors

        updateState {
            copy(
                sortType = sortType,
                results = results,
                error = if (results == null) ItemsError.dataLoading else null,
                snapshot = results.freeze(), lce = LCE2.Content,
            )
        }
    }

    private fun startObserving(results: RealmResults<RItem>) {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> {items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                  updateState {
                      copy(snapshot = items.freeze(), lce = LCE2.Content,)
                  }
                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    val correctedModifications = org.zotero.android.database.Database.correctedModifications(modifications = modifications, insertions = insertions, deletions = deletions)

                    processUpdate(items = items, deletions = deletions, insertions = insertions, modifications = correctedModifications)
                    updateState {
                        copy(snapshot = items.freeze(), lce = LCE2.Content,)
                    }

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
            callPoint = SinglePickerResult.CallPoint.AllItems
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

    fun cacheItemAccessory(item: RItem) {
        if (viewState.itemAccessories[item.key] != null) {
            return
        }
        val accessory = accessory(item) ?: return

        val mutableItemAccessories = viewState.itemAccessories.toMutableMap()
        mutableItemAccessories.put(item.key, accessory)
        updateState {
            copy(itemAccessories = mutableItemAccessories)
        }
    }

    fun cellAccessory(accessory: ItemAccessory?): ItemCellModel.Accessory? {
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

    private fun showMetadata(item: RItem) {
        showItemDetail(item)
//        resetActiveSearch() //TODO implement
    }

    fun onItemTapped(item: RItem) {
        if (viewState.isEditing) {
            selectItem(item.key)
            return
        }

        val accessory = viewState.itemAccessories[item.key]
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
}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val snapshot: RealmResults<RItem>? = null,
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
    val itemAccessories: Map<String, ItemAccessory> = emptyMap(),
    val keys: List<String> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val isEditing: Boolean = false,
    val error: ItemsError? = null,
    val results: RealmResults<RItem>? = null,
    val shouldShowAddBottomSheet: Boolean = false,
    val searchTerm: String? = null,
    val sortType: ItemsSortType = ItemsSortType.default,
    val itemKeyToDuplicate: String? = null,
    val updateItemKey: String? = null,
    val processingBibliography: Boolean = false,
    val bibliographyError: Throwable? = null,
    val attachmentToOpen: String? = null,
    val downloadBatchData: ItemsState.DownloadBatchData? = null
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
    object ShowItemDetailEffect: AllItemsViewEffect()
    object ShowAddOrEditNoteEffect: AllItemsViewEffect()
    object ShowItemTypePickerEffect : AllItemsViewEffect()
    data class OpenWebpage(val uri: Uri) : AllItemsViewEffect()
    data class OpenFile(val file: File, val mimeType: String) : AllItemsViewEffect()
    object ShowVideoPlayer : AllItemsViewEffect()
    object ShowImageViewer : AllItemsViewEffect()
    data class ShowPdf(val file: File) : AllItemsViewEffect()
    object ScreenRefresh : AllItemsViewEffect()
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
