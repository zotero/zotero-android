package org.zotero.android.screens.itemdetails

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.ObjectChangeSet
import io.realm.RealmObjectChangeListener
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.joda.time.DateTime
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.emptyImmutableSet
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.ARG_ITEM_DETAILS_SCREEN
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.CancelParentCreationDbRequest
import org.zotero.android.database.requests.CreateAttachmentsDbRequest
import org.zotero.android.database.requests.CreateItemFromDetailDbRequest
import org.zotero.android.database.requests.CreateNoteDbRequest
import org.zotero.android.database.requests.DeleteCreatorItemDetailDbRequest
import org.zotero.android.database.requests.DeleteObjectsDbRequest
import org.zotero.android.database.requests.DeleteTagFromItemDbRequest
import org.zotero.android.database.requests.EditCreatorItemDetailDbRequest
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditNoteDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.EditTypeItemDetailDbRequest
import org.zotero.android.database.requests.EndItemDetailEditingDbRequest
import org.zotero.android.database.requests.MarkItemsAsTrashedDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.RemoveItemFromParentDbRequest
import org.zotero.android.database.requests.ReorderCreatorsItemDetailDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.helpers.MediaSelectionResult
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.helpers.formatter.dateFormatItemDetails
import org.zotero.android.helpers.formatter.fullDateWithDashes
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.helpers.formatter.sqlFormat
import org.zotero.android.ktx.index
import org.zotero.android.pdf.data.PdfReaderArgs
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.collections.data.LibrariesAndCollectionsBackButtonActiveEvent
import org.zotero.android.screens.creatoredit.data.CreatorEditArgs
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.NavigateToPdfScreen
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.OnBack
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.OpenFile
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.OpenWebpage
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ScreenRefresh
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ShowAddOrEditNoteEffect
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ShowCreatorEditEffect
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ShowImageViewer
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ShowItemTypePickerEffect
import org.zotero.android.screens.itemdetails.ItemDetailsViewEffect.ShowVideoPlayer
import org.zotero.android.screens.itemdetails.data.DeleteCreatorAction
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.screens.itemdetails.data.ItemDetailAttachmentKind
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.data.ItemDetailData
import org.zotero.android.screens.itemdetails.data.ItemDetailError
import org.zotero.android.screens.itemdetails.data.ItemDetailField
import org.zotero.android.screens.itemdetails.data.ItemDetailsArgs
import org.zotero.android.screens.mediaviewer.image.ImageViewerArgs
import org.zotero.android.screens.mediaviewer.video.VideoPlayerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.AttachmentFileCleanupController
import org.zotero.android.sync.AttachmentFileDeletedNotification
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.ItemDetailCreateDataResult
import org.zotero.android.sync.ItemDetailDataCreator
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import org.zotero.android.sync.conflictresolution.AskUserToDeleteOrRestoreItem
import org.zotero.android.sync.conflictresolution.ConflictResolutionUseCase
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionsHolder
import org.zotero.android.uicomponents.reorder.move
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerStateCreator
import timber.log.Timber
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.inject.Inject

const val numberOfRowsInLazyColumnBeforeListOfCreatorsStarts = 1

@HiltViewModel
class ItemDetailsViewModel @Inject constructor(
    dispatcher: CoroutineDispatcher,
    private val defaults: Defaults,
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val urlDetector: UrlDetector,
    private val schemaController: SchemaController,
    private val selectMedia: SelectMediaUseCase,
    private val fileDownloader: AttachmentDownloader,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    private val fileCleanupController: AttachmentFileCleanupController,
    private val conflictResolutionUseCase: ConflictResolutionUseCase,
    private val dateParser: DateParser,
    private val context: Context,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<ItemDetailsViewState, ItemDetailsViewEffect>(ItemDetailsViewState()) {

    val screenArgs: ItemDetailsArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_ITEM_DETAILS_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private var coroutineScope = CoroutineScope(dispatcher)

    //required to keep item change listener alive
    private var currentItem: RItem? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(askUserToDeleteOrRestoreItem: AskUserToDeleteOrRestoreItem) {
        updateState {
            copy(error = ItemDetailError.askUserToDeleteOrRestoreItem)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(itemDetailCreator: ItemDetailCreator) {
        onSaveCreator(itemDetailCreator)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(action: DeleteCreatorAction) {
        onDeleteCreator(action.id)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(saveNoteAction: SaveNoteAction) {
        if (!saveNoteAction.isFromDashboard) {
            viewModelScope.launch {
                saveNote(saveNoteAction.text, saveNoteAction.tags, saveNoteAction.key)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.ItemDetails) {
            viewModelScope.launch {
                changeType(singlePickerResult.id)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.ItemDetails) {
            viewModelScope.launch {
                setTags(tagPickerResult.tags)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(attachmentFileDeleted: EventBusConstants.AttachmentFileDeleted) {
        updateDeletedAttachmentFiles(attachmentFileDeleted.notification)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: EventBusConstants.FileWasSelected) {
        if (event.uri != null && event.callPoint == EventBusConstants.FileWasSelected.CallPoint.ItemDetails) {
            viewModelScope.launch {
                addAttachments(listOf(event.uri))
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        initViewState(screenArgs)
        EventBus.getDefault().post(LibrariesAndCollectionsBackButtonActiveEvent(false))

        setupFileObservers()
        setupOnFieldValueTextChangeFlow()
        setupOnAbstractTextChangeFlow()

        loadInitialData()
    }

    private fun setupFileObservers() {
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                process(update)
                if (update.kind is AttachmentDownloader.Update.Kind.progress) {
                    return@onEach
                }
                if (viewState.attachmentToOpen != update.key) {
                    return@onEach
                }

                attachmentOpened(update.key)
                when (update.kind) {
                    AttachmentDownloader.Update.Kind.ready -> {
                        showAttachment(key = update.key, parentKey = update.parentKey ,libraryId = update.libraryId)
                    }
                    is AttachmentDownloader.Update.Kind.failed -> {
                        //TODO implement when unzipping is supported
                    }
                    else -> {}
                }
            }
            .launchIn(coroutineScope)

    }

    fun attachmentOpened(key: String) {
        if (viewState.attachmentToOpen != key) {
            return
        }
        viewModelScope.launch {
            updateState {
                copy(attachmentToOpen = null)
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().post(LibrariesAndCollectionsBackButtonActiveEvent(true))
        currentItem?.removeAllChangeListeners()
        EventBus.getDefault().unregister(this)
        conflictResolutionUseCase.currentlyDisplayedItemLibraryIdentifier = null
        conflictResolutionUseCase.currentlyDisplayedItemKey = null

        coroutineScope.cancel()
        super.onCleared()
    }

    private fun initViewState(args: ItemDetailsArgs) {
        val type = args.type
        val library = args.library
        val preScrolledChildKey = args.childKey
        val userId = defaults.getUserId()

        when (type) {
            is DetailType.preview -> {
                updateState {
                    copy(
                        key = type.key,
                        isEditing = false
                    )
                }

            }
            is DetailType.creation, is DetailType.duplication -> {
                updateState {
                    copy(
                        key = KeyGenerator.newKey(),
                        isEditing = true,
                    )
                }

            }

        }
        updateState {
            copy(
                type = type,
                userId = userId,
                library = library,
                preScrolledChildKey = preScrolledChildKey,
                abstractText = viewState.data.abstract ?: ""
            )
        }
        conflictResolutionUseCase.currentlyDisplayedItemLibraryIdentifier = viewState.library?.identifier
        conflictResolutionUseCase.currentlyDisplayedItemKey = viewState.key

    }

    fun onSaveOrEditClicked() {
        if (viewState.isEditing) {
            endEditing()
        } else {
            val updatedStateData = viewState.data.deepCopy()
            val updatedData = viewState.data.deepCopy(
                fieldIds = ItemDetailDataCreator.allFieldKeys(
                    viewState.data.type,
                    schemaController = this.schemaController
                )
            )
            updateState {
                copy(
                    snapshot = updatedStateData,
                    data = updatedData,
                    isEditing = true,
                    abstractText = updatedData.abstract ?: ""
                )
            }
        }


    }

    private fun loadInitialData() {
        val key = viewState.key
        val libraryId = viewState.library!!.identifier
        var collectionKey: String?
        var data: ItemDetailCreateDataResult?

        try {
            val type = viewState.type
            when (type) {
                is DetailType.creation -> {
                    val itemType = type.type
                    val child = type.child
                    collectionKey = type.collectionKey
                    data = ItemDetailDataCreator.createData(
                        ItemDetailDataCreator.Kind.new(
                            itemType = itemType,
                            child = child
                        ),
                        schemaController = this.schemaController,
                        fileStorage = this.fileStore,
                        urlDetector = this.urlDetector,
                        dateParser = this.dateParser,
                        doiDetector = { doiValue -> FieldKeys.Item.isDoi(doiValue) },
                        defaults = this.defaults
                    )
                }
                is DetailType.preview -> {
                    reloadData(isEditing = viewState.isEditing)
                    return
                }
                is DetailType.duplication -> {
                    val itemKey = type.itemKey
                    val _collectionKey = type.collectionKey
                    collectionKey = _collectionKey
                    val item = dbWrapperMain.realmDbStorage.perform(
                        request = ReadItemDbRequest(
                            libraryId = viewState.library!!.identifier,
                            key = itemKey
                        )
                    )
                    data = ItemDetailDataCreator.createData(
                        ItemDetailDataCreator.Kind.existing(
                            item = item,
                            ignoreChildren = true
                        ),
                        schemaController = this.schemaController,
                        fileStorage = this.fileStore,
                        urlDetector = this.urlDetector,
                        dateParser = this.dateParser,
                        doiDetector = { doiValue -> FieldKeys.Item.isDoi(doiValue) },
                        defaults = this.defaults
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "can't load initial data ")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
            return
        }
        val request = CreateItemFromDetailDbRequest(
            key = key,
            libraryId = libraryId,
            collectionKey = collectionKey,
            data = data.itemData,
            attachments = data.attachments,
            notes = data.notes,
            tags = data.tags,
            dateParser = this.dateParser,
            schemaController = this.schemaController,
            fileStore = fileStore
        )
        viewModelScope.launch {
            val result = perform(dbWrapperMain, request = request, invalidateRealm = true)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't create initial item")
                updateState {
                    copy(error = ItemDetailError.cantCreateData)
                }
            } else {
                reloadData(isEditing = true)
            }
        }

    }

    private fun reloadData(isEditing: Boolean) = viewModelScope.launch {
        try {
            currentItem?.removeAllChangeListeners()
            val item = dbWrapperMain.realmDbStorage.perform(
                request = ReadItemDbRequest(
                    libraryId = viewState.library!!.identifier,
                    key = viewState.key
                ), refreshRealm = true
            )
            currentItem = item
            if (viewState.type is DetailType.preview) {
                item.addChangeListener(RealmObjectChangeListener<RItem> { items, changeSet ->
                    if (changeSet?.changedFields?.any {
                            RItem.observableKeypathsForItemDetail.contains(
                                it
                            )
                        } == true) {
                        itemChanged(items, changeSet)
                    }
                })
            }


            val (data, attachments, notes, tags) = ItemDetailDataCreator.createData(
                ItemDetailDataCreator.Kind.existing(item = item, ignoreChildren = false),
                schemaController = this@ItemDetailsViewModel.schemaController,
                dateParser = this@ItemDetailsViewModel.dateParser,
                fileStorage = this@ItemDetailsViewModel.fileStore,
                urlDetector = this@ItemDetailsViewModel.urlDetector,
                doiDetector = { doiValue -> FieldKeys.Item.isDoi(doiValue) },
                defaults = this@ItemDetailsViewModel.defaults
            )

            if (!isEditing) {
                data.fieldIds =
                    ItemDetailDataCreator.filteredFieldKeys(data.fieldIds, fields = data.fields)
            }

            saveReloaded(
                data = data,
                attachments = attachments,
                notes = notes,
                tags = tags,
                isEditing = isEditing
            )
        } catch (e: Exception) {
            Timber.e(e, "ItemDetailActionHandler: can't load data")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
        }
    }

    private fun saveReloaded(
        data: ItemDetailData,
        attachments: List<Attachment>,
        notes: List<Note>,
        tags: List<Tag>,
        isEditing: Boolean,
    ) {
        updateState {
            copy(
                data = data,
                abstractText = data.abstract ?: ""
            )
        }
        if (viewState.snapshot != null || isEditing) {
            val updatedSnapshot = data.deepCopy(
                fieldIds = ItemDetailDataCreator.filteredFieldKeys(
                    data.fieldIds,
                    fields = data.fields
                )
            )
            updateState {
                copy(
                    snapshot = updatedSnapshot,
                )
            }
        }
        updateState {
            copy(
                attachments = attachments.toPersistentList(),
                notes = notes.toPersistentList(),
                tags = tags.toPersistentList(),
                isLoadingData = false,
                isEditing = isEditing,
            )
        }
        //Update UI and hide progress.
    }

    private fun itemChanged(item: RItem, changeSet: ObjectChangeSet) {
        if (changeSet.isDeleted) {
            return
        }

        if (shouldReloadData(item, changeSet.changedFields)) {
            itemChanged(viewState)
        }
    }

    private var ignoreScreenRefreshOnNextDbUpdate: Boolean = false

    private fun shouldReloadData(item: RItem, changes: Array<String>): Boolean {
        if (changes.contains("version")) {
            if (changes.contains("changeType")) {
                //Unfortunately there is no way on Android's RealmDB to get the previous value of RealmObject's 'changeType' field in it's ChangeListener.
                // That's why we have to adjust shouldReloadData logic to ignore user's input during DB Refresh with this flag.
                if (ignoreScreenRefreshOnNextDbUpdate) {
                    ignoreScreenRefreshOnNextDbUpdate = false
                    return false
                }
                return true
            }
            return false
        }

        if (changes.contains("children")) {
            return true
        }
        return false
    }

    private fun itemChanged(state: ItemDetailsViewState) {
        if (!state.isEditing) {
            reloadData(viewState.isEditing)
            return
        }
        updateState {
            copy(error = ItemDetailError.itemWasChangedRemotely)
        }
    }

    fun onAddCreator() {
        showCreatorCreation(itemType = viewState.data.type)
    }

    fun onAddNote() {
        openNoteEditor(null)
    }

    fun onAddTag() {
        val libraryId = viewState.library!!.identifier
        val selected = viewState.tags.map { it.id }.toSet()
        ScreenArguments.tagPickerArgs =
            TagPickerArgs(libraryId = libraryId, selectedTags = selected, callPoint = TagPickerResult.CallPoint.ItemDetails)
        triggerEffect(ItemDetailsViewEffect.ShowTagPickerEffect)
    }

    fun onAddAttachment() {
        triggerEffect(ItemDetailsViewEffect.AddAttachment)
    }

    fun showCreatorCreation(itemType: String) {
        val schema = schemaController.creators(itemType)?.firstOrNull { it.primary }
        if (schema == null) {
            return
        }
        val localized = schemaController.localizedCreator(schema.creatorType)
        if (localized == null) {
            return
        }
        val creator = ItemDetailCreator.init(
            type = schema.creatorType, primary = schema.primary,
            localizedType = localized, namePresentation = defaults.getCreatorNamePresentation()
        )
        _showCreatorEditor(creator, itemType = itemType, isEditing = false)
    }

    private fun showCreatorEditor(creator: ItemDetailCreator, itemType: String) {
        _showCreatorEditor(creator, itemType = itemType, isEditing = true)
    }

    private fun _showCreatorEditor(
        creator: ItemDetailCreator,
        itemType: String,
        isEditing: Boolean
    ) {
        ScreenArguments.creatorEditArgs =
            CreatorEditArgs(creator = creator, itemType = itemType, isEditing = isEditing)
        triggerEffect(ShowCreatorEditEffect)
    }

    private fun onSaveCreator(creator: ItemDetailCreator) {
        viewModelScope.launch {
            if (!viewState.data.creatorIds.contains(creator.id)) {
                updateState {
                    copy(
                        data = viewState.data.deepCopy(
                            creatorIds = viewState.data.creatorIds + creator.id,
                        )
                    )
                }
            }
            val updatedCreators = viewState.data.creators.toMutableMap()
            updatedCreators[creator.id] = creator
            updateState {
                copy(
                    data = viewState.data.deepCopy(
                        creators = updatedCreators
                    )
                )
            }

            val orderId = viewState.data.creatorIds.indexOfFirst { it == creator.id }
            if (orderId == -1) {
                return@launch
            }
            val request = EditCreatorItemDetailDbRequest(
                key = viewState.key,
                libraryId = viewState.library!!.identifier,
                creator = creator,
                orderId = orderId
            )

            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't create creator")
            }
        }
    }

    private var onAbstractTextChangeFlow = MutableStateFlow<String?>(null)

    private fun setupOnAbstractTextChangeFlow() {
        onAbstractTextChangeFlow
            .debounce(500)
            .map { data ->
                if (data != null) {
                    onAbstractEdit(data)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAbstractTextChange(newAbstract: String) {
        updateState {
            copy(
                abstractText = newAbstract
            )
        }
        onAbstractTextChangeFlow.tryEmit(newAbstract)
    }


    private var onFieldValueTextChangeFlow = MutableStateFlow<Pair<String, String>?>(null)

    private fun setupOnFieldValueTextChangeFlow() {
        onFieldValueTextChangeFlow
            .debounce(500)
            .map { data ->
                if (data != null) {
                    setFieldValue(data.first, data.second)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onFieldFocusFieldChange(fieldId: String) {
        val changeFlowValue = onFieldValueTextChangeFlow.value
        if (changeFlowValue != null) {
            setFieldValue(changeFlowValue.first, changeFlowValue.second)
        }
        val field = viewState.data.fields[fieldId] ?: return
        updateState {
            copy(
                fieldFocusKey = fieldId,
                fieldFocusText = field.valueOrAdditionalInfo
            )
        }
    }

    fun onFieldValueTextChange(id: String, value: String) {
        updateState {
            copy(fieldFocusText = value)
        }
        onFieldValueTextChangeFlow.tryEmit(id to value)
    }

    private fun setFieldValue(id: String, value: String) {
        val field = viewState.data.fields[id]
        if (field == null) {
            return
        }
        ignoreScreenRefreshOnNextDbUpdate = true

        field.value = value
        field.isTappable = ItemDetailDataCreator.isTappable(
            key = field.key,
            value = field.value,
            urlDetector = this.urlDetector,
            doiDetector = { doiValue -> FieldKeys.Item.isDoi(doiValue) }
        )

        if (field.key == FieldKeys.Item.date || field.baseField == FieldKeys.Item.date) {
            val order = this.dateParser.parse(value)?.orderWithSpaces
            if (order != null) {
                val info = (field.additionalInfo ?: emptyMap()).toMutableMap()
                info[ItemDetailField.AdditionalInfoKey.dateOrder] = order
                field.additionalInfo = info
            } else if (field.additionalInfo != null) {
                field.additionalInfo = null
            }
        } else if (field.additionalInfo != null) {
            field.additionalInfo = null
        }

        val updatedData = viewState.data.deepCopy(fields = viewState.data.fields)
        updateState {
            copy(data = updatedData)
        }

        val request = EditItemFieldsDbRequest(
            key = viewState.key,
            libraryId = viewState.library!!.identifier,
            fieldValues = mapOf(
                KeyBaseKeyPair(
                    key = field.key,
                    baseKey = field.baseField
                ) to field.value
            ),
            dateParser = this.dateParser
        )

        viewModelScope.launch {
            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't store field")
            }
        }

        triggerEffect(ScreenRefresh)
    }

    fun onTitleEdit(newTitle: String) {
        val key = this.schemaController.titleKey(viewState.data.type)
        if (key == null) {
            Timber.e("ItemDetailActionHandler: schema controller doesn't contain title key for item type ${viewState.data.type}")
            return
        }

        val updatedData = viewState.data.deepCopy(title = newTitle)
        updateState {
            copy(data = updatedData)
        }

        val keyPair = KeyBaseKeyPair(
            key = key,
            baseKey = (if (key != FieldKeys.Item.title) FieldKeys.Item.title else null)
        )
        val request = EditItemFieldsDbRequest(
            key = viewState.key,
            libraryId = viewState.library!!.identifier,
            fieldValues = mapOf(keyPair to newTitle),
            dateParser = this.dateParser
        )
        viewModelScope.launch {
            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't store title")
            }
        }
    }

    private fun onAbstractEdit(newAbstract: String) {
        ignoreScreenRefreshOnNextDbUpdate = true
        val updatedData = viewState.data.deepCopy(abstract = newAbstract)
        updateState {
            copy(data = updatedData)
        }

        val request = EditItemFieldsDbRequest(
            key = viewState.key,
            libraryId = viewState.library!!.identifier,
            fieldValues = mapOf(
                KeyBaseKeyPair(
                    key = FieldKeys.Item.abstractN,
                    baseKey = null
                ) to newAbstract
            ),
            dateParser = this.dateParser
        )

        viewModelScope.launch {
            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't store abstract")
            }
        }
    }

    private fun parseDateSpecialValue(value: String): Date? {
        return when (value.lowercase()) {
            "tomorrow" ->
                DateTime.now().plusDays(1).toDate()

            "today" ->
                Date()

            "yesterday" ->
                DateTime.now().minusDays(1).toDate()

            else ->
                null
        }
    }

    private fun updated(dateField: ItemDetailField): ItemDetailField? {
        val date = parseDateSpecialValue(dateField.value) ?: return null
        val field = dateField
        field.value = fullDateWithDashes.format(date)
        val order = this.dateParser.parse(field.value)?.orderWithSpaces
        if (order != null) {
            val mutableAdditionalInfo = (field.additionalInfo ?: emptyMap()).toMutableMap()
            mutableAdditionalInfo[ItemDetailField.AdditionalInfoKey.dateOrder] = order
            field.additionalInfo = mutableAdditionalInfo
        }

//        val updatedFieldsMap = viewState.data.fields.toMutableMap()
//        updatedFieldsMap[field.key] = field

//        val updatedData = viewState.data.deepCopy(fields = updatedFieldsMap)
//        updateState {
//            copy(data = updatedData)
//        }
        return field
    }

    private fun endEditing() {
        ignoreScreenRefreshOnNextDbUpdate = false
        if (viewState.snapshot == viewState.data) {
            return
        }
        try {
            coroutineScope.launch {
                endEditingAsync()
            }
        } catch (error: Exception) {
            Timber.e(error, "ItemDetailStore: can't store changes")
            updateState {
                copy(error = error as? ItemDetailError ?: ItemDetailError.cantStoreChanges)
            }
        }
    }

    private fun endEditingAsync() {
        viewModelScope.launch {
            val updatedFields: MutableMap<KeyBaseKeyPair, String> = mutableMapOf()
            val updatedFieldsMap = viewState.data.fields.toMutableMap()
            val fieldAccessDate = viewState.data.fields[FieldKeys.Item.accessDate]
            if (fieldAccessDate != null) {
                val updated = updated(accessDateField = fieldAccessDate, originalField = viewState.snapshot?.fields?.get(FieldKeys.Item.accessDate))
                if (updated.value != fieldAccessDate.value) {
                    updatedFields[KeyBaseKeyPair(key = updated.key, baseKey = updated.baseField)] = updated.value
                    updatedFieldsMap[updated.key] = updated
                }
            }
            val itemDateField = viewState.data.fields.values.firstOrNull { it.baseField == FieldKeys.Item.date || it.key == FieldKeys.Item.date }

            if(itemDateField != null) {
                val updated = updated(dateField = itemDateField)
                if (updated != null) {
                    if (updated.value != itemDateField.value) {
                        updatedFields[KeyBaseKeyPair(key=  updated.key, baseKey = updated.baseField)] = updated.value
                        updatedFieldsMap[updated.key] = updated
                    }
                }

            }

            val requests: MutableList<DbRequest> = mutableListOf(
                EndItemDetailEditingDbRequest(
                    libraryId = viewState.library!!.identifier,
                    itemKey = viewState.key
                )
            )
            if (!updatedFields.isEmpty()) {
                requests.add(
                    element = EditItemFieldsDbRequest(
                        key = viewState.key,
                        libraryId = viewState.library!!.identifier,
                        fieldValues = updatedFields,
                        dateParser = this@ItemDetailsViewModel.dateParser
                    ), index = 0
                )
            }
            perform(
                dbWrapper = dbWrapperMain,
                writeRequests = requests
            ).ifFailure {
                Timber.e(it)
                return@launch
            }

            val updatedData = viewState.data.deepCopy(
                fields = updatedFieldsMap,
                fieldIds = ItemDetailDataCreator.filteredFieldKeys(
                    viewState.data.fieldIds,
                    viewState.data.fields,
                ),
                dateModified = Date()
            )
            updateState {
                copy(
                    snapshot = null,
                    isEditing = false,
                    type = DetailType.preview(viewState.key),
                    data = updatedData,
                    abstractText = updatedData.abstract ?: ""
                )
            }
        }

    }

    private fun updated(accessDateField: ItemDetailField, originalField: ItemDetailField?): ItemDetailField {
        var field = accessDateField

        var date: Date? = null
        val dateSpecific = parseDateSpecialValue(field.value)
        if (dateSpecific != null) {
            date = dateSpecific
        } else {
            try {
                val _date = sqlFormat.parse(field.value)
                if (_date != null) {
                    date = _date
                }
            } catch (e: Exception) {
                //no-op
            }

        }

        if (date != null) {
            field.value = iso8601DateFormatV2.format(date)
            field.additionalInfo = mapOf(
                ItemDetailField.AdditionalInfoKey.formattedDate to dateFormatItemDetails().format(date),
                ItemDetailField.AdditionalInfoKey.formattedEditDate to sqlFormat.format(date)
            )
        } else {
            if (originalField != null) {
                field = originalField
            } else {
                field.value = ""
                field.additionalInfo = emptyMap()
            }
        }

//        val updatedFieldsMap = viewState.data.fields.toMutableMap()
//        updatedFieldsMap[field.key] = field

//        val updatedData = viewState.data.deepCopy(fields = updatedFieldsMap)
//        viewModelScope.launch {
//            updateState {
//                copy(data = updatedData)
//            }
//        }
        return field
    }

    private fun process(update: AttachmentDownloader.Update) {
        if (viewState.library!!.identifier != update.libraryId) {
            return
        }

        val index = viewState.attachments.indexOfFirst {  it.key == update.key }
        if (index == -1) {
            return
        }
        val attachment = viewState.attachments[index]
        viewModelScope.launch {
            when (update.kind) {
                AttachmentDownloader.Update.Kind.cancelled, is AttachmentDownloader.Update.Kind.failed, is AttachmentDownloader.Update.Kind.progress -> {
                    updateState {
                        copy(updateAttachmentKey = attachment.key)
                    }
                }
                AttachmentDownloader.Update.Kind.ready -> {
                    val new = attachment.changed(location = Attachment.FileLocation.local)
                    if (new == null) {
                        return@launch
                    }
                    val attachmentsMutable = viewState.attachments.toMutableList()
                    attachmentsMutable[index] = new
                    updateState {
                        copy(attachments = attachmentsMutable.toPersistentList(), updateAttachmentKey = new.key)
                    }
                }
            }
            triggerEffect(ScreenRefresh)
        }
    }

    fun onCancelOrBackClicked() {
        if (viewState.isEditing) {
            cancelChanges()
        } else {
            triggerEffect(OnBack)
        }
    }

    private fun cancelChanges() {
        viewModelScope.launch {
            when (val type = viewState.type) {
                is DetailType.duplication -> {
                    perform(
                        dbWrapper = dbWrapperMain,
                        request = DeleteObjectsDbRequest(
                            clazz = RItem::class,
                            keys = listOf(viewState.key),
                            libraryId = viewState.library!!.identifier
                        )
                    ).ifFailure {
                        Timber.e(it, "ItemDetailActionHandler: can't remove duplicated and cancelled item")
                        updateState {
                            copy(error = ItemDetailError.cantRemoveItem)
                        }
                        return@launch
                    }
                    triggerEffect(OnBack)
                }

                is DetailType.creation -> {
                    val child = type.child
                    val actions: MutableList<DbRequest> = mutableListOf(
                        DeleteObjectsDbRequest(
                            clazz = RItem::class,
                            keys = listOf(viewState.key),
                            libraryId = viewState.library!!.identifier
                        )
                    )
                    if (child != null) {
                        actions.add(
                            element = CancelParentCreationDbRequest(
                                key = child.key,
                                libraryId = child.libraryId
                            ), index = 0
                        )
                    }
                    perform(
                        dbWrapper = dbWrapperMain,
                        writeRequests = actions
                    ).ifFailure {
                        Timber.e(it, "ItemDetailActionHandler: can't remove created and cancelled item")
                        updateState {
                            copy(error = ItemDetailError.cantRemoveItem)
                        }
                        return@launch
                    }
                    triggerEffect(OnBack)
                }
                is DetailType.preview -> {
                    //no-op
                }
            }
        }
    }

    fun onCreatorClicked(creator: ItemDetailCreator) {
        showCreatorEditor(creator = creator, itemType = viewState.data.type)
    }

    fun onDeleteCreator(creatorId: String) {
        viewModelScope.launch {
            val index = viewState.data.creatorIds.indexOf(creatorId)
            if (index == -1) {
                return@launch
            }
            val updatedCreators = viewState.data.creators.toMutableMap()
            updatedCreators.remove(creatorId)
            updateState {
                copy(
                    data = viewState.data.deepCopy(
                        creatorIds = viewState.data.creatorIds - creatorId,
                        creators = updatedCreators
                    )
                )
            }

            val request = DeleteCreatorItemDetailDbRequest(
                key = viewState.key,
                libraryId = viewState.library!!.identifier,
                creatorId = creatorId
            )

            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e("ItemDetailActionHandler: can't delete creator")
            }
        }
    }

    fun openNoteEditor(note: Note?) {
        val library = viewState.library!!
        val key = note?.key ?: KeyGenerator.newKey()
        val title =
            AddOrEditNoteArgs.TitleData(type = viewState.data.type, title = viewState.data.title)

        val args = AddOrEditNoteArgs(
            title = title,
            key = key,
            libraryId = library.identifier,
            readOnly = !library.metadataEditable,
            isFromDashboard = false
        )
        val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(
            data = args,
            charset = StandardCharsets.UTF_8
        )
        triggerEffect(ShowAddOrEditNoteEffect(encodedArgs))
    }

    private suspend fun saveNote(text: String, tags: List<Tag>, key: String) {
        val oldNote = viewState.notes.firstOrNull { it.key == key }
        val note = Note(key = key, text = text, tags = tags)

        val indexNote = viewState.notes.indexOfFirst { it.key == key }
        val updatedNotes = viewState.notes.toMutableList()
        if (indexNote != -1) {
            updatedNotes[indexNote] = note
        } else {
            updatedNotes.add(note)
        }
        updateState {
            copy(
                notes = updatedNotes.toPersistentList(),
                backgroundProcessedItems = (backgroundProcessedItems + key).toImmutableSet()
            )
        }

        fun finishSave(error: Throwable?) {
            updateState {
                copy(backgroundProcessedItems = (backgroundProcessedItems - key).toImmutableSet())
            }
            if (error == null) {
                return
            }

            Timber.e(error, "Can't edit/save note $key")
            updateState {
                copy(error = ItemDetailError.cantSaveNote)
            }
            val index = viewState.notes.indexOfFirst { it.key == key }
            if (index == -1) {
                return
            }
            val updatedNotes = viewState.notes.toMutableList()
            if (oldNote != null) {
                updatedNotes[index] = oldNote
            } else {
                updatedNotes.removeAt(index)
            }
            updateState {
                copy(notes = updatedNotes.toPersistentList())
            }
        }

        if (oldNote != null) {
            val request = EditNoteDbRequest (note = note, libraryId = viewState.library!!.identifier)
            perform(dbWrapper = dbWrapperMain, request = request).ifFailure {
                finishSave(it)
                return
            }
            return
        }

        val type = schemaController.localizedItemType(ItemTypes.note) ?: ItemTypes.note
        val request = CreateNoteDbRequest(
            note = note,
            localizedType = type,
            libraryId = viewState.library!!.identifier,
            collectionKey = null,
            parentKey = viewState.key
        )
        perform(
            dbWrapper = dbWrapperMain,
            request = request,
            invalidateRealm = true
        ).ifFailure { error ->
            finishSave(error)
            return
        }
        finishSave(null)
    }

    fun openAttachment(attachment: Attachment) {
        val key = attachment.key
        val (progress, _) = this.fileDownloader.data(
            key = key,
            libraryId = viewState.library!!.identifier
        )

        if (progress != null) {
            if (viewState.attachmentToOpen == key) {
                updateState {
                    copy(attachmentToOpen = null)
                }
            }

            this.fileDownloader.cancel(key = key, libraryId = viewState.library!!.identifier)
            return
        }
        val attachment = viewState.attachments.firstOrNull { it.key == key }
        if (attachment == null) {
            return
        }

        updateState {
            copy(attachmentToOpen = key)
        }
        this.fileDownloader.downloadIfNeeded(attachment = attachment, parentKey = viewState.key)
    }

    fun onItemTypeClicked() {
        if (viewState.data.isAttachment) {
            return
        }
        val selected = viewState.data.type
        ScreenArguments.singlePickerArgs = SinglePickerArgs(
            singlePickerState = SinglePickerStateCreator.create(
                selected = selected,
                schemaController
            ),
            showSaveButton = false,
            title = context.getString(Strings.item_type),
            callPoint = SinglePickerResult.CallPoint.ItemDetails
        )
        triggerEffect(ShowItemTypePickerEffect)
    }

    private fun changeType(newType: String) {
        val itemData: ItemDetailData
        try {
            itemData = data(newType, viewState.data)
        } catch (error: Throwable) {
            Timber.e(error)
            updateState {
                copy(
                    error = (error as? ItemDetailError) ?: ItemDetailError.typeNotSupported(newType)
                )
            }
            return
        }

        val droppedFields = droppedFields(viewState.data, itemData)
        updateState {
            if (droppedFields.isEmpty()) {
                copy(data = itemData)
            } else {
                copy(
                    promptSnapshot = itemData,
                    error = ItemDetailError.droppedFields(droppedFields)
                )
            }
        }
        if (droppedFields.isEmpty()) {
            changeTypeInDb()
        }
    }

    private fun droppedFields(fromData: ItemDetailData, toData: ItemDetailData): List<String> {
        val newFields = toData.fields.values.toMutableSet()
        val subtracted = fromData.fields.values.filter{ !it.value.isEmpty() }.toMutableSet()
        for (field in newFields) {
            val oldField = subtracted.firstOrNull {(it.baseField ?: it.name) == (field.baseField ?: field.name) }
            if (oldField == null) {
                continue
            }
            subtracted.remove(oldField)
        }
        return subtracted.map{ it.name }.sorted()
    }

    private fun creators(type: String, originalData: Map<String, ItemDetailCreator>): Map<String, ItemDetailCreator> {
        val schemas = schemaController.creators(type)
        if (schemas == null) {
            throw ItemDetailError.typeNotSupported(type)
        }
        val primary = schemas.firstOrNull { it.primary }
        if (primary == null) {
            throw ItemDetailError.typeNotSupported(type)
        }

        val creators = originalData.toMutableMap()
        for ((key, originalCreator) in originalData) {
            if(schemas.firstOrNull{ it.creatorType == originalCreator.type } != null) {
                continue
            }

            val creator = originalCreator.copy()

            if (originalCreator.primary) {
                creator.type = primary.creatorType
            } else {
                creator.type = "contributor"
            }
            creator.localizedType = schemaController.localizedCreator(creator.type) ?: ""
            creators[key] = creator
        }

        return creators
    }

    private fun data(type: String, originalData: ItemDetailData): ItemDetailData {
        val localizedType = schemaController.localizedItemType(itemType = type)
        if (localizedType == null) {
            throw ItemDetailError.typeNotSupported(type)
        }

        val (fieldIds, fields, hasAbstract) = ItemDetailDataCreator.fieldData(
            type,
            schemaController = this.schemaController,
            urlDetector = this.urlDetector,
            doiDetector = { FieldKeys.Item.isDoi(it) },
            getExistingData = { key, baseField ->
                val originalDataField = originalData.fields[key]
                if (originalDataField != null) {
                    return@fieldData originalDataField.name to originalDataField.value
                }
                val base = baseField
                if (base == null) {
                    return@fieldData null to null
                }

                val field = originalData.fields.values.firstOrNull { it.baseField == base }
                if (field != null) {
                    return@fieldData null to field.value
                }
                return@fieldData null to null
            },
            dateParser = this.dateParser
        )

        val data = originalData
            .deepCopy(
                type = type,
                isAttachment = type == ItemTypes.attachment,
                localizedType = localizedType,
                fields = fields,
                fieldIds = fieldIds,
                abstract = if (hasAbstract) (originalData.abstract ?: "") else null,
                creators = creators(type, originalData.creators),
                creatorIds = originalData.creatorIds,
            )
        return data
    }

    fun onDismissErrorDialog() {
        updateState {
            copy(
                error = null,
            )
        }
    }

    fun acceptPrompt() {
        val snapshot = viewState.promptSnapshot
        if (snapshot == null) {
            return
        }
        updateState {
            copy(
                data = snapshot.deepCopy(),
                promptSnapshot = null
            )
        }
        changeTypeInDb()
    }

    fun acceptItemWasChangedRemotely() {
        reloadData(viewState.isEditing)
    }

    fun deleteOrRestoreItem(isDelete: Boolean) {
        conflictResolutionUseCase.deleteOrRestoreItem(isDelete = isDelete, key = viewState.key)
        if (isDelete) {
            triggerEffect(OnBack)
        }
    }

    fun cancelPrompt() {
        updateState {
            copy(
                promptSnapshot = null,
            )
        }
    }

    fun dismissBottomSheet() {
        updateState {
            copy(longPressOptionsHolder = null)
        }
    }

    fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        viewModelScope.launch {
            when (longPressOptionItem) {
                is LongPressOptionItem.TrashNote -> {
                    delete(longPressOptionItem.note)
                }
                is LongPressOptionItem.DeleteTag -> {
                    delete(longPressOptionItem.tag)
                }
                is LongPressOptionItem.DeleteCreator -> {
                    delete(longPressOptionItem.creator)
                }
                is LongPressOptionItem.MoveToTrashAttachment -> {
                    delete(longPressOptionItem.attachment)
                }
                is LongPressOptionItem.DeleteAttachmentFile -> {
                    deleteFile(longPressOptionItem.attachment)
                }
                is LongPressOptionItem.MoveToStandaloneAttachment -> {
                    moveToStandalone(longPressOptionItem.attachment)
                }
                else -> {}
            }
        }
    }

    fun onNoteLongClick(note: Note) {
        updateState {
            copy(
                longPressOptionsHolder = LongPressOptionsHolder(
                    title = note.title,
                    longPressOptionItems = listOf(LongPressOptionItem.TrashNote(note))
                )
            )
        }
    }
    fun onTagLongClick(tag: Tag) {
        updateState {
            copy(
                longPressOptionsHolder = LongPressOptionsHolder(
                    title = tag.name,
                    longPressOptionItems = listOf(LongPressOptionItem.DeleteTag(tag))
                )
            )
        }
    }

    fun onCreatorLongClick(creator: ItemDetailCreator) {
        updateState {
            copy(
                longPressOptionsHolder = LongPressOptionsHolder(
                    title = creator.name,
                    longPressOptionItems = listOf(LongPressOptionItem.DeleteCreator(creator))
                )
            )
        }
    }

    fun onAttachmentLongClick(attachment: Attachment) {
        val actions = mutableListOf<LongPressOptionItem>()
        val attachmentType = attachment.type
        if (attachmentType is Attachment.Kind.file && attachmentType.location == Attachment.FileLocation.local) {
            actions.add(LongPressOptionItem.DeleteAttachmentFile(attachment))
        }

        if (!viewState.data.isAttachment) {
            actions.add(LongPressOptionItem.MoveToStandaloneAttachment(attachment))
            actions.add(LongPressOptionItem.MoveToTrashAttachment(attachment))
        }
        if (actions.isNotEmpty()) {
            updateState {
                copy(
                    longPressOptionsHolder = LongPressOptionsHolder(
                        title = attachment.title,
                        longPressOptionItems = actions
                    )
                )
            }
        }
    }

    private suspend fun delete(tag: Tag) {
        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems + tag.name).toImmutableSet())
        }
        val request = DeleteTagFromItemDbRequest(
            key = viewState.key,
            libraryId = viewState.library!!.identifier,
            tagName = tag.name
        )
        perform(dbWrapper = dbWrapperMain, request = request).ifFailure { error ->
            Timber.e(error, "ItemDetailActionHandler: can't delete tag ${tag.name}")
            updateState {
                copy(
                    error = ItemDetailError.cantSaveTags,
                    backgroundProcessedItems = (backgroundProcessedItems - tag.name).toImmutableSet()
                )
            }
            return
        }
        val index = viewState.tags.indexOf(tag)
        if (index != -1) {
            val updatedTags = viewState.tags.toMutableList()
            updatedTags.removeAt(index)
            updateState {
                copy(
                    tags = updatedTags.toPersistentList(),
                    backgroundProcessedItems = (backgroundProcessedItems - tag.name).toPersistentSet()
                )
            }
        }
    }

    private suspend fun delete(note: Note) {
        if (!viewState.notes.contains(note)) {
            return
        }
        trashItem(key = note.key) {
            val index = viewState.notes.indexOf(note)
            if (index == -1) {
                return@trashItem
            }
            val updatedNotes = viewState.notes.toMutableList()
            updatedNotes.removeAt(index)
            updateState {
                copy(notes = updatedNotes.toPersistentList())
            }
        }
    }

    private fun delete(creator: ItemDetailCreator) {
        val id = creator.id

        val index = viewState.data.creatorIds.indexOf(id)
        if (index == -1) {
            return
        }
        val updatedCreatorIds = viewState.data.creatorIds.toMutableList()
        updatedCreatorIds.removeAt(index)

        val updatedCreators = viewState.data.creators.toMutableMap()
        updatedCreators.remove(id)

        updateState {
            copy(data = data.deepCopy(creatorIds = updatedCreatorIds, creators = updatedCreators))
        }
    }

    private fun deleteFile(attachment: Attachment) {
        this.fileCleanupController.delete(
            AttachmentFileCleanupController.DeletionType.individual(
                attachment = attachment,
                parentKey = viewState.key
            ), completed = null
        )
    }

    private suspend fun delete(attachment: Attachment) {
        if (!viewState.attachments.contains(attachment)) {
            return
        }

        trashItem(key = attachment.key) {
            val index = viewState.attachments.indexOf(attachment)
            if (index == -1) {
                return@trashItem
            }
            val updatedAttachments = viewState.attachments.toMutableList()
            updatedAttachments.removeAt(index)
            updateState {
                copy(attachments = updatedAttachments.toPersistentList())
            }
        }
    }

    private suspend fun trashItem(key: String, onSuccess: () -> Unit) {
        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems + key).toImmutableSet())
        }
        val request = MarkItemsAsTrashedDbRequest(
            keys = listOf(key),
            libraryId = viewState.library!!.identifier,
            trashed = true
        )
        perform(dbWrapper = dbWrapperMain, request = request).ifFailure { error ->
            Timber.e(error, "ItemDetailActionHandler: can't trash item $key")
            updateState {
                copy(
                    error = ItemDetailError.cantTrashItem,
                    backgroundProcessedItems = (backgroundProcessedItems - key).toImmutableSet()
                )
            }
            return
        }
        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems - key).toImmutableSet())
        }
        onSuccess()

    }

    private suspend fun showAttachment(
        key: String,
        parentKey: String?,
        libraryId: LibraryIdentifier
    ) {
        val attachmentResult = attachment(key = key, libraryId = libraryId)
        if (attachmentResult == null) {
            return
        }
        val (attachment, library) = attachmentResult
        viewModelScope.launch {
            show(attachment = attachment, parentKey = parentKey, library = library)
        }
    }

    fun attachment(key: String, libraryId: LibraryIdentifier): Pair<Attachment, Library>? {
        val index = viewState.attachments.indexOfFirst { it.key == key && it.libraryId == libraryId }
        if (index == -1) {
            return null
        }
        val attachment = viewState.attachments[index]
        val library = viewState.library!!
        return attachment to library
    }

    private suspend fun show(attachment: Attachment, parentKey: String?, library: Library) {
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
                        showPdf(file = file, parentKey = parentKey, attachment = attachment)
                    }
                    "text/html", "text/plain" -> {
                        val url = file.toUri().toString()
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        triggerEffect(ItemDetailsViewEffect.ShowZoteroWebView(encodedUrl))
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

    private suspend fun showUrl(url: String) {
        val uri = url.toUri()
        if (uri.scheme != null && uri.scheme != "http" && uri.scheme != "https") {
            val mimeType = getUriDetailsUseCase.getMimeType(url)!!
            triggerEffect(OpenFile(uri.toFile(), mimeType))
        } else {
            triggerEffect(OpenWebpage(url))
        }
    }

    private fun showPdf(file: File, parentKey: String?, attachment: Attachment) {
        val uri = file.toUri()
        val pdfReaderArgs = PdfReaderArgs(
            key = attachment.key,
            parentKey = parentKey,
            library = viewState.library!!,
            page = null,
            preselectedAnnotationKey = null,
            uri = uri,
        )
        val params = navigationParamsMarshaller.encodeObjectToBase64(pdfReaderArgs, StandardCharsets.UTF_8)
        triggerEffect(NavigateToPdfScreen(params))
    }

    private fun openFile(file: File, mime: String) {
        triggerEffect(OpenFile(file, mime))
    }

    private fun showVideoFile(file: File) {
        ScreenArguments.videoPlayerArgs = VideoPlayerArgs(Uri.fromFile(file))
        triggerEffect(ShowVideoPlayer)
    }

    private fun showImageFile(file: File) {
        ScreenArguments.imageViewerArgs = ImageViewerArgs(Uri.fromFile(file), file.name)
        triggerEffect(ShowImageViewer)
    }

    fun calculateAttachmentKind(attachment: Attachment): ItemDetailAttachmentKind {
        val isProcessing = viewState.backgroundProcessedItems.contains(attachment.key)
        if (isProcessing) {
            return ItemDetailAttachmentKind.disabled
        }

        val (progress, error) = fileDownloader.data(key = attachment.key, libraryId = attachment.libraryId)

        if (error != null) {
            return ItemDetailAttachmentKind.failed(error)
        }

        if (progress != null) {
            return ItemDetailAttachmentKind.inProgress(progress)
        }

        return ItemDetailAttachmentKind.default
    }

    private fun updateDeletedAttachmentFiles(notification: AttachmentFileDeletedNotification) {
        when (notification) {
            AttachmentFileDeletedNotification.all -> {
                if(viewState.attachments.firstOrNull{ it.location == Attachment.FileLocation.local } == null) {
                    return
                }
                setAllAttachmentFilesAsDeleted()
            }
            is AttachmentFileDeletedNotification.library -> {
                val libraryId = notification.libraryId
                if (libraryId != viewState.library!!.identifier || viewState.attachments.firstOrNull { it.location == Attachment.FileLocation.local } == null) {
                    return
                }
                setAllAttachmentFilesAsDeleted()
            }
            is AttachmentFileDeletedNotification.allForItems -> {
                val libraryId = notification.libraryId
                val keys = notification.keys
                if (libraryId != viewState.library!!.identifier
                    || !keys.contains(viewState.key)
                    || viewState.attachments.firstOrNull { it.location == Attachment.FileLocation.local } == null) {
                    return
                }
                setAllAttachmentFilesAsDeleted()
            }
            is AttachmentFileDeletedNotification.individual -> {
                val libraryId = notification.libraryId
                val key = notification.key
                val index = viewState.attachments.indexOfFirst {  it.key == key && it.libraryId == libraryId }
                if (index == -1) {
                    return
                }
                val new = viewState.attachments[index].changed(location = Attachment.FileLocation.remote, condition = { it == Attachment.FileLocation.local })
                if (new == null) {
                    return
                }
                val updatedAttachments = viewState.attachments.toMutableList()
                updatedAttachments[index] = new
                updateState {
                    copy(
                        attachments = updatedAttachments.toPersistentList(),
                        updateAttachmentKey = new.key
                    )
                }
            }
        }
    }

    private fun setAllAttachmentFilesAsDeleted() {
        val updatedAttachments = viewState.attachments.toMutableList()
        for ((index, attachment) in viewState.attachments.withIndex()) {
            val new = attachment.changed(
                location = Attachment.FileLocation.remote,
                condition = { it == Attachment.FileLocation.local })
            if (new == null) {
                continue
            }
            updatedAttachments[index] = new
        }
        updateState {
            copy(attachments = updatedAttachments.toPersistentList())
        }
    }

    private suspend fun setTags(tags: List<Tag>) {
        val oldTags = viewState.tags
        updateState {
            copy(
                tags = tags.toPersistentList(),
                backgroundProcessedItems = (backgroundProcessedItems + tags.map { it.name }).toImmutableSet()
            )
        }

        val request = EditTagsForItemDbRequest(key = viewState.key, libraryId = viewState.library!!.identifier, tags = tags)
        val result = perform(dbWrapper = dbWrapperMain, request = request)

        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems - tags.map { it.name }).toImmutableSet())
        }
        if (result is Result.Failure) {
            Timber.e(result.exception, "ItemDetailActionHandler: can't set tags to item")
            updateState {
                copy(tags = oldTags)
            }
        }
    }

    private suspend fun addAttachments(urls: List<Uri>) {
        createAttachments(urls, libraryId = viewState.library!!.identifier) { attachments, failedCopyNames ->
            if (attachments.isEmpty()) {
                updateState {
                    copy(error = ItemDetailError.cantAddAttachments(ItemDetailError.AttachmentAddError.couldNotMoveFromSource(failedCopyNames)))
                }
                return@createAttachments
            }
            for (attachment in attachments) {
                val index = viewState.attachments.index(
                    attachment,
                    sortedBy = { first, second ->
                        first.title.compareTo(
                            second.title,
                            ignoreCase = true
                        ) == 1
                    })
                val updatedAttachments = viewState.attachments.toMutableList()
                updatedAttachments.add(index, attachment)
                updateState {
                    copy(
                        attachments = updatedAttachments.toPersistentList(),
                        backgroundProcessedItems = (backgroundProcessedItems + attachment.key).toImmutableSet()
                    )
                }
            }

            if (!failedCopyNames.isEmpty()) {
                updateState {
                    copy(
                        error = ItemDetailError.cantAddAttachments(
                            ItemDetailError.AttachmentAddError.couldNotMoveFromSource(
                                failedCopyNames
                            )
                        )
                    )
                }
            }

            val type = this.schemaController.localizedItemType(itemType = ItemTypes.attachment)
                ?: ItemTypes.attachment
            val request = CreateAttachmentsDbRequest(
                attachments = attachments,
                parentKey = viewState.key,
                localizedType = type,
                collections = emptySet(),
                fileStore = fileStore
            )

            viewModelScope.launch {
                val result =
                    perform(dbWrapperMain, invalidateRealm = true, request = request)
                for (attachment in attachments) {
                    updateState {
                        copy(backgroundProcessedItems = (backgroundProcessedItems - attachment.key).toImmutableSet())
                    }
                }

                if (result is Result.Failure) {
                    Timber.e(
                        result.exception,
                        "ItemDetailActionHandler: could not create attachments"
                    )

                    val updatedAttachments = viewState.attachments.toMutableList()
                    updatedAttachments.removeAll { stateAttachment ->
                        attachments.map { it.key }.contains(stateAttachment.key)
                    }

                    updateState {
                        copy(
                            attachments = updatedAttachments.toPersistentList(),
                            error = ItemDetailError.cantAddAttachments(ItemDetailError.AttachmentAddError.allFailedCreation)
                        )
                    }
                } else if (result is Result.Success) {
                    val failed = result.value
                    if (failed.isEmpty()) {
                        return@launch
                    }
                    val updatedAttachments = viewState.attachments.toMutableList()
                    updatedAttachments.removeAll { stateAttachment ->
                        failed.map { it.first }.contains(stateAttachment.key)
                    }

                    updateState {
                        copy(
                            attachments = updatedAttachments.toPersistentList(),
                            error = ItemDetailError.cantAddAttachments(
                                ItemDetailError.AttachmentAddError.someFailedCreation(
                                    failed.map { it.second })
                            )
                        )
                    }
                }
            }
        }

    }

    private suspend fun createAttachments(urls: List<Uri>, libraryId: LibraryIdentifier, completion:  (List<Attachment>, List<String>) -> Unit) {
        val attachments = mutableListOf<Attachment>()
        val failedNames = mutableListOf<String>()
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
            val nameWithExtension = original.nameWithoutExtension + "." + original.extension
            val mimeType =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(original.extension)
                    ?: "application/octet-stream"
            val file = fileStore.attachmentFile(
                libraryId = libraryId,
                key = key,
                filename = nameWithExtension,
            )
            if (!original.renameTo(file)) {
                Timber.e("ItemDetailActionHandler: can't move attachment from source url $url")
                failedNames.add(nameWithExtension)
            } else {
                attachments.add(
                    Attachment(
                        type = Attachment.Kind.file(
                            filename = nameWithExtension,
                            contentType = mimeType,
                            location = Attachment.FileLocation.local,
                            linkType = Attachment.FileLinkType.importedFile
                        ),
                        title = nameWithExtension,
                        key = key,
                        libraryId = libraryId
                    )
                )
            }
        }
        completion(attachments, failedNames)
    }

    private suspend fun moveToStandalone(attachment: Attachment) {
        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems + attachment.key).toImmutableSet())
        }

        val result = perform(
            dbWrapperMain,
            request = RemoveItemFromParentDbRequest(
                key = attachment.key,
                libraryId = attachment.libraryId
            )
        )

        updateState {
            copy(backgroundProcessedItems = (backgroundProcessedItems - attachment.key).toImmutableSet())
        }
        if (result is Result.Failure) {
            Timber.e(
                result.exception,
                "ItemDetailActionHandler: can't move attachment to standalone"
            )
            updateState {
                copy(error = ItemDetailError.cantRemoveParent)
            }
        } else {
            updateState {
                copy(attachments = (attachments - attachment).toPersistentList())
            }

        }
    }

    fun onRowTapped(field: ItemDetailField) {
        if (!(!viewState.isEditing || viewState.data.type == ItemTypes.attachment) || !field.isTappable) return
        when (field.key) {
            FieldKeys.Item.Attachment.url -> {
                triggerEffect(OpenWebpage(field.value))
            }
            FieldKeys.Item.doi -> {
                val encoded = FieldKeys.Item.clean(doi = field.value)
                showDoi(encoded)
            }
        }
    }

    private fun showDoi(doi: String) {
        val url = "https://doi.org/$doi"
        triggerEffect(OpenWebpage(url))
    }

    fun onMove(from: Int, to: Int) {
        viewModelScope.launch {
            //We need to adjust index by the number of 'rows' we have in LazyColumn before the list of creators start.
            val adjustedFrom = from - numberOfRowsInLazyColumnBeforeListOfCreatorsStarts
            val adjustedTo = to - numberOfRowsInLazyColumnBeforeListOfCreatorsStarts
            val updatedCreators = viewState.data.creatorIds
            updateState {
                copy(
                    data = viewState.data.deepCopy(
                        creatorIds = updatedCreators.move(
                            IntRange(adjustedFrom, adjustedFrom),
                            adjustedTo
                        )
                    ),
                )
            }

            val request = ReorderCreatorsItemDetailDbRequest(
                key = viewState.key,
                libraryId = viewState.library!!.identifier,
                ids = viewState.data.creatorIds
            )

            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't reorder creators")
            }

            triggerEffect(ScreenRefresh)
        }
    }

    private fun changeTypeInDb() {
        val request = EditTypeItemDetailDbRequest(
            key = viewState.key,
            libraryId = viewState.library!!.identifier,
            type = viewState.data.type,
            fields = viewState.data.databaseFields(schemaController = schemaController),
            creatorIds = viewState.data.creatorIds,
            creators = viewState.data.creators,
            dateParser = this.dateParser
        )

        viewModelScope.launch {
            val result = perform(dbWrapper = dbWrapperMain, request = request)
            if (result is Result.Failure) {
                Timber.e(result.exception, "ItemDetailActionHandler: can't change type")
            }
        }
    }
}

data class ItemDetailsViewState(
    val key: String = "",
    val library: Library? = null,
    val userId: Long = 0,
    val type: DetailType = DetailType.preview(""),
    val preScrolledChildKey: String? = null,
    val isEditing: Boolean = false,
    var error: ItemDetailError? = null,
    val data: ItemDetailData = ItemDetailData.empty,
    var snapshot: ItemDetailData? = null,
    var promptSnapshot: ItemDetailData? = null,
    var notes: PersistentList<Note> = persistentListOf(),
    var attachments: PersistentList<Attachment> = persistentListOf(),
    var tags: PersistentList<Tag> = persistentListOf(),
    var updateAttachmentKey: String? = null,
    var attachmentToOpen: String? = null,
    var isLoadingData: Boolean = false,
    var backgroundProcessedItems: ImmutableSet<String> = emptyImmutableSet(),
    val longPressOptionsHolder: LongPressOptionsHolder? = null,
    val fieldFocusKey: String? = null,
    val fieldFocusText: String = "",
    val abstractText: String = "",
) : ViewState

sealed class ItemDetailsViewEffect : ViewEffect {
    object ShowCreatorEditEffect : ItemDetailsViewEffect()
    object ShowTagPickerEffect : ItemDetailsViewEffect()
    object ShowItemTypePickerEffect : ItemDetailsViewEffect()
    object ScreenRefresh : ItemDetailsViewEffect()
    object OnBack : ItemDetailsViewEffect()
    data class ShowAddOrEditNoteEffect(val screenArgs: String) : ItemDetailsViewEffect()
    object ShowVideoPlayer : ItemDetailsViewEffect()
    object ShowImageViewer : ItemDetailsViewEffect()
    data class OpenFile(val file: File, val mimeType: String) : ItemDetailsViewEffect()
    data class NavigateToPdfScreen(val params: String) : ItemDetailsViewEffect()
    data class OpenWebpage(val url: String) : ItemDetailsViewEffect()
    data class ShowZoteroWebView(val url: String) : ItemDetailsViewEffect()
    object AddAttachment : ItemDetailsViewEffect()
}
