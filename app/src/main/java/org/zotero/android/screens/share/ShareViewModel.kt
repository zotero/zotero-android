package org.zotero.android.screens.share

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.BuildConfig
import org.zotero.android.api.mappers.CreatorResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.TagResponseMapper
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.backgrounduploader.BackgroundUpload
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.CreateAttachmentDbRequest
import org.zotero.android.database.requests.CreateBackendItemDbRequest
import org.zotero.android.database.requests.CreateItemWithAttachmentDbRequest
import org.zotero.android.database.requests.MarkAttachmentUploadedDbRequest
import org.zotero.android.database.requests.ReadCollectionAndLibraryDbRequest
import org.zotero.android.database.requests.ReadGroupDbRequest
import org.zotero.android.database.requests.ReadRecentCollections
import org.zotero.android.database.requests.UpdateCollectionLastUsedDbRequest
import org.zotero.android.database.requests.UpdateVersionType
import org.zotero.android.database.requests.UpdateVersionsDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.screens.share.backgroundprocessor.BackgroundUploadProcessor
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.ItemPickerState
import org.zotero.android.screens.share.data.ProcessedAttachment
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.screens.share.data.UploadData
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerArgs
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerResults
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareSubmissionData
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SchemaError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.SyncObservableEventStream
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.sync.Tag
import org.zotero.android.sync.syncactions.AuthorizeUploadSyncAction
import org.zotero.android.sync.syncactions.SubmitUpdateSyncAction
import org.zotero.android.sync.syncactions.data.AuthorizeUploadResponse
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.translator.data.TranslatorAction
import org.zotero.android.translator.data.TranslatorActionEventStream
import org.zotero.android.translator.web.TranslatorWebCallChainExecutor
import org.zotero.android.translator.web.TranslatorWebExtractionExecutor
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
internal class ShareViewModel @Inject constructor(
    dispatchers: Dispatchers,
    private val translatorWebExtractionExecutor: TranslatorWebExtractionExecutor,
    private val translatorWebCallChainExecutor: TranslatorWebCallChainExecutor,
    private val shareRawAttachmentLoader: ShareRawAttachmentLoader,
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    private val fileStore: FileStore,
    private val syncScheduler: SyncScheduler,
    private val syncObservableEventStream: SyncObservableEventStream,
    private val translatorActionEventStream: TranslatorActionEventStream,
    private val dbWrapper: DbWrapper,
    private val itemResponseMapper: ItemResponseMapper,
    private val schemaController: SchemaController,
    private val tagResponseMapper: TagResponseMapper,
    private val creatorResponseMapper: CreatorResponseMapper,
    private val defaults: Defaults,
    private val context: Context,
    private val dateParser: DateParser,
    private val backgroundUploadProcessor: BackgroundUploadProcessor,
) : BaseViewModel2<ShareViewState, ShareViewEffect>(ShareViewState()) {

    private val defaultLibraryId: LibraryIdentifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
    private val defaultExtension = "pdf"
    private val defaultMimetype = "application/pdf"
    private val zipMimetype = "application/zip"

    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var selectedCollectionId: CollectionIdentifier
    private lateinit var selectedLibraryId: LibraryIdentifier
    private lateinit var attachmentKey: String

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.ShareScreen) {
            updateState {
                copy(tags = tagPickerResult.tags)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ShareCollectionPickerResults) {
       set(collection = result.collection, library = result.library)
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        selectedCollectionId = fileStore.getSelectedCollectionId()
        selectedLibraryId = fileStore.getSelectedLibrary()
        attachmentKey = KeyGenerator.newKey()
        setupObservers()
        ioCoroutineScope.launch {
            try {
                syncScheduler.startSyncController(type = SyncKind.collectionsOnly, libraries = Libraries.all, retryAttempt = 0)
                val attachment = shareRawAttachmentLoader.getLoadedAttachmentResult()
                process(attachment)
            } catch (e: Exception) {
                Timber.e(e, "ExtensionViewModel: could not load attachment")
                updateAttachmentState(
                    AttachmentState.failed(
                        attachmentError(
                            generalError = CustomResult.GeneralError.CodeError(e),
                            libraryId = null
                        )
                    )
                )
            }
        }
    }

    private fun setupObservers() {
        syncObservableEventStream.flow()
            .onEach { data ->
                finishSync(successful = (data == null))
            }
            .launchIn(viewModelScope)

        translatorActionEventStream.flow()
            .onEach { event ->
                if (event is Result.Failure) {
                    Timber.e(event.exception, "ExtensionViewModel: web view error")
                    updateState {
                        copy(
                            attachmentState = AttachmentState.failed(
                                attachmentError(
                                    CustomResult.GeneralError.CodeError(
                                        event.exception
                                    ), libraryId = null
                                )
                            )
                        )
                    }
                    return@onEach
                }
                event as Result.Success
                val eventValue = event.value
                when (eventValue) {
                    is TranslatorAction.loadedItems -> {
                        val data = eventValue.data
                        val cookies = eventValue.cookies
                        val userAgent = eventValue.userAgent
                        val referrer = eventValue.referrer
                        Timber.i("webview action - loaded ${data.size()} zotero items")
                        processItems(
                            data = data,
                            cookies = cookies,
                            userAgent = userAgent,
                            referrer = referrer
                        )
                    }

                    is TranslatorAction.selectItem -> {
                        Timber.i("webview action - loaded ${eventValue.data.size} list items")
                        updateState {
                            copy(
                                itemPickerState = ItemPickerState(
                                    items = eventValue.data,
                                    picked = null
                                )
                            )
                        }
                    }

                    is TranslatorAction.reportProgress -> {
                        Timber.i("webview action - progress ${eventValue.progress}")
                        updateState {
                            copy(attachmentState = AttachmentState.translating(eventValue.progress))
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

    }

    private fun parse(data: JsonArray): Pair<ItemResponse, JsonObject?> {
        val itemData = data.firstOrNull() ?: throw AttachmentState.Error.itemsNotFound
        var item = itemResponseMapper.fromTranslatorResponse(
            response = itemData.asJsonObject,
            schemaController = schemaController,
            tagResponseMapper = tagResponseMapper,
            creatorResponseMapper = creatorResponseMapper
        )
        if (!item.tags.isEmpty()) {
            item = item.copyWithAutomaticTags
        }
        var attachment: JsonObject? = null
        if (defaults.isShareExtensionIncludeAttachment()) {
            val itemDataJsonObject = itemData.asJsonObject
            attachment =
                itemDataJsonObject["attachments"]?.asJsonArray?.firstOrNull { it.asJsonObject["mimeType"]?.asString == this.defaultMimetype }?.asJsonObject
        }

        return item to attachment
    }

    private fun processItems(
        data: JsonArray,
        cookies: String?,
        userAgent: String?,
        referrer: String?
    ) {

        val item: ItemResponse
        var attachment: JsonObject? = null

        try {
            Timber.i("parse zotero items")
            val (_item, _attachment) = parse(data)
            item = _item
            attachment = _attachment
        } catch (error: Exception) {
            Timber.e(error, "ShareViewModel: could not process item ")
            updateState {
                copy(
                    attachmentState = AttachmentState.failed(
                        attachmentError(
                            CustomResult.GeneralError.CodeError(
                                error
                            ), libraryId = null
                        )
                    )
                )
            }
            return
        }
        if (attachment == null || !attachment.has("url")) {
            Timber.i("parsed item without attachment")
            updateState {
                copy(
                    processedAttachment = ProcessedAttachment.item(item),
                    expectedItem = item,
                    attachmentState = AttachmentState.processed
                )
            }
            return
        }
        val url = attachment["url"]
        Timber.i("ShareViewModel: parsed item with attachment, download attachment")

        val file =
            fileStore.shareExtensionDownload(key = this.attachmentKey, ext = this.defaultExtension)
        download(
            item = item,
            attachment = attachment,
            attachmentUrl = url,
            file = file,
            cookies = cookies,
            userAgent = userAgent,
            referrer = referrer
        )
    }

    private fun download(
        item: ItemResponse,
        attachment: JsonObject,
        attachmentUrl: JsonElement?,
        file: File,
        cookies: String?,
        userAgent: String?,
        referrer: String?
    ) {

        //TODO implement download
    }

    private fun finishSync(successful: Boolean) {
        if (!successful) {
            updateState {
                copy(collectionPickerState = CollectionPickerState.failed)
            }
            return
        }


        try {
            var library: Library? = null
            var collection: Collection? = null
            var recents = mutableListOf<RecentData>()
            dbWrapper.realmDbStorage.perform { coordinator ->
                val request = ReadCollectionAndLibraryDbRequest(
                    collectionId = this.selectedCollectionId,
                    libraryId = this.selectedLibraryId
                )
                val (_collection, _library) = coordinator.perform(request = request)

                val recentCollections =
                    coordinator.perform(request = ReadRecentCollections(excluding = null))

                recents = recentCollections.toMutableList()
                library = _library
                when (this.selectedCollectionId) {
                    is CollectionIdentifier.collection -> {
                        collection = _collection
                    }

                    else -> {
                        //no-op
                    }
                }
            }
            if (library == null) {
                return
            }

            if (!recents.any { it.collection?.identifier == collection?.identifier && it.library.identifier == library!!.identifier }) {
                recents.add(
                    index = 0,
                    element = RecentData(
                        collection = collection,
                        library = library!!,
                        isRecent = false
                    )
                )
            }

            updateState {
                copy(
                    collectionPickerState = CollectionPickerState.picked(
                        library = library!!,
                        collection = collection
                    ),
                    recents = recents
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "can't load collections")
            val library = Library(
                identifier = this.defaultLibraryId,
                name = RCustomLibraryType.myLibrary.libraryName,
                metadataEditable = true,
                filesEditable = true
            )
            updateState {
                copy(
                    collectionPickerState = CollectionPickerState.picked(
                        library = library,
                        collection = null
                    )
                )
            }
        }
    }

    private suspend fun process(url: String) {
        try {
            val attachment = translatorWebExtractionExecutor.execute(url = url)
            process(attachment)
        } catch (e: Exception) {
            Timber.e(e, "ExtensionViewModel: webview could not load data")
            updateAttachmentState(
                AttachmentState.failed(
                    attachmentError(
                        generalError = CustomResult.GeneralError.CodeError(e),
                        libraryId = null
                    )
                )
            )
        }
    }

    private suspend fun process(attachment: RawAttachment) {
        when (attachment) {
            is RawAttachment.web -> {
                processWeb(
                    title = attachment.title,
                    url = attachment.url,
                    html = attachment.html,
                    cookies = attachment.cookies,
                    frames = attachment.frames,
                    userAgent = attachment.userAgent,
                    referrer = attachment.referrer
                )
            }
            is RawAttachment.remoteUrl -> {
                process(url = attachment.url)
            }
            is RawAttachment.fileUrl -> {
                process(uri = attachment.uri)
            }
            else -> {

            }
        }
    }

    private suspend fun process(uri: Uri) {
        val fileName = getUriDetailsUseCase.getFullName(uri)
        val fileExtension =  getUriDetailsUseCase.getExtension(uri)
        if (fileName == null || fileExtension == null) {
            viewModelScope.launch {
                updateState {
                    copy(attachmentState = AttachmentState.failed(AttachmentState.Error.fileMissing))
                }
            }
            return
        }
        val tmpFile = fileStore.temporaryFile(fileExtension)
        try {
            getUriDetailsUseCase.copyFile(uri, tmpFile)
            viewModelScope.launch {
                updateState {
                    copy(
                        processedAttachment = ProcessedAttachment.file(
                            file = tmpFile,
                            fileName = fileName
                        ),
                        expectedAttachment = fileName to tmpFile,
                        attachmentState = AttachmentState.processed
                    )
                }
            }

        } catch (e: Exception) {
            Timber.e(e)
            viewModelScope.launch {
                updateState {
                    copy(attachmentState = AttachmentState.failed(AttachmentState.Error.fileMissing))
                }
            }
        }
    }

    private fun processWeb(
        title: String,
        url: String,
        html: String,
        cookies: String,
        frames: List<String>,
        userAgent: String,
        referrer: String
    ) {
        viewModelScope.launch {
            updateState {
                copy(
                    title = title,
                    url = url
                )
            }
        }

        translatorWebCallChainExecutor.translate(
            url = url,
            html = html,
            cookies = cookies,
            frames = frames,
            userAgent = userAgent,
            referrer = referrer
        )

    }

    private fun updateAttachmentState(attachmentState: AttachmentState) {
        viewModelScope.launch {
            updateState {
                copy(attachmentState = attachmentState)
            }
        }
    }

    private fun attachmentError(
        generalError: CustomResult.GeneralError,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        when (generalError) {
            is CustomResult.GeneralError.CodeError -> {
                val error = generalError.throwable
                if (error is AttachmentState.Error) {
                    return error
                }
                if (error is Parsing.Error) {
                    Timber.e(error, "ExtensionViewModel: could not parse item")
                    return AttachmentState.Error.parseError(error)
                }

                if (error is SchemaError) {
                    Timber.e(error, "ExtensionViewModel: schema failed")
                    return AttachmentState.Error.schemaError(error)
                }
                if (error is TranslationWebViewError) {
                    return AttachmentState.Error.webViewError(error)
                }
            }

            is CustomResult.GeneralError.NetworkError -> {
                return networkErrorRequiresAbort(
                    error = generalError,
                    url = BuildConfig.BASE_API_URL,
                    libraryId = libraryId
                )
            }
        }
        return AttachmentState.Error.unknown
    }

    private fun networkErrorRequiresAbort(
        error: CustomResult.GeneralError.NetworkError,
        url: String?,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        val defaultError = if ((url ?: "").contains(BuildConfig.BASE_API_URL)) {
            AttachmentState.Error.apiFailure
        } else {
            AttachmentState.Error.webDavFailure
        }

        val code = error.httpCode
        if (code == 413  && libraryId != null) {
            return AttachmentState.Error.quotaLimit(libraryId)
        }
        return defaultError
    }

    fun setFromRecent(collection: Collection?, library: Library) {
        updateSelected(collection = collection, library=  library)
    }
    private fun updateSelected(collection: Collection?, library: Library) {
        this.selectedLibraryId = library.identifier
        this.selectedCollectionId = collection?.identifier ?: Collection.initWithCustomType(
            CollectionIdentifier.CustomType.all
        ).identifier
        updateState {
            copy(collectionPickerState = CollectionPickerState.picked(library, collection))
        }
        fileStore.setSelectedCollectionId(this.selectedCollectionId)
        fileStore.setSelectedLibrary(this.selectedLibraryId)
    }

    fun itemTitle(item: ItemResponse, defaultValue: String): String {
        return schemaController.titleKey(item.rawType)
            ?.let { item.fields[KeyBaseKeyPair(key = it, baseKey = null)] } ?: defaultValue
    }

    fun navigateToTagPicker() {
        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = this.selectedLibraryId,
            selectedTags = viewState.tags.map { it.name }.toSet(),
            tags = emptyList(),
            callPoint = TagPickerResult.CallPoint.ShareScreen,
        )
        triggerEffect(ShareViewEffect.NavigateToTagPickerScreen)
    }

    fun navigateToCollectionPicker() {
        ScreenArguments.shareCollectionPickerArgs = ShareCollectionPickerArgs(
            selectedCollectionId = this.selectedCollectionId,
            selectedLibraryId = this.selectedLibraryId,
        )
        triggerEffect(ShareViewEffect.NavigateToCollectionPickerScreen)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private fun set(collection: Collection?, library: Library) {
        updateSelected(collection = collection, library = library)
        val new =
            viewState.recents.firstOrNull { it.collection?.identifier == collection?.identifier && it.library.identifier == library.identifier }
        if (new != null) {
            if (new.isRecent && !viewState.recents[0].isRecent) {
                val mutableRecents = viewState.recents.toMutableList()
                mutableRecents.removeFirst()
                updateState {
                    copy(recents = mutableRecents)
                }
            }
        } else {
            val mutableRecents = viewState.recents.toMutableList()
            if (!viewState.recents[0].isRecent) {
                mutableRecents[0] =
                    RecentData(collection = collection, library = library, isRecent = false)
            } else {
                mutableRecents.add(
                    0,
                    RecentData(collection = collection, library = library, isRecent = false)
                )
            }
            updateState {
                copy(recents = mutableRecents)
            }
        }
    }

    fun submitAsync() {
        viewModelScope.launch {
            submit()
        }
    }

    private suspend fun submit() {
        if (!viewState.attachmentState.isSubmittable) {
            Timber.e("Tried to submit unsubmittable state")
            return
        }

        updateState {
            copy(isSubmitting = true)
        }

        val tags = viewState.tags.map { TagResponse(tag = it.name, type = it.type) }
        val libraryId: LibraryIdentifier
        val collectionKeys: Set<String>
        val userId = defaults.getUserId()
        val collectionPickerState = viewState.collectionPickerState
        when (collectionPickerState) {
            is CollectionPickerState.picked -> {
                libraryId = collectionPickerState.library.identifier
                collectionKeys =
                    collectionPickerState.collection?.identifier?.keyGet?.let { setOf(it) }
                        ?: emptySet()
            }

            else -> {
                libraryId = this.defaultLibraryId
                collectionKeys = emptySet()
            }
        }
        var attachment = viewState.processedAttachment

        if (attachment != null) {
            when (attachment) {
                is ProcessedAttachment.item -> {
                    attachment = ProcessedAttachment.item(
                        attachment.item.copy(
                            libraryId = libraryId,
                            collectionKeys = collectionKeys,
                            tags = if (defaults.isShareExtensionIncludeAttachment()) {
                                attachment.item.tags + tags
                            } else {
                                tags
                            }
                        )
                    )
                }

                is ProcessedAttachment.itemWithAttachment -> {
                    val item = attachment.item
                    val attachmentData = attachment.attachment
                    val attachmentFile = attachment.attachmentFile
                    val newTags = if (defaults.isShareExtensionIncludeAttachment()) {
                        item.tags + tags
                    } else {
                        tags
                    }
                    attachment = ProcessedAttachment.itemWithAttachment(
                        item = item.copy(
                            libraryId = libraryId,
                            collectionKeys = collectionKeys,
                            tags = newTags
                        ),
                        attachment = attachmentData,
                        attachmentFile = attachmentFile
                    )

                }

                is ProcessedAttachment.file -> {
                    //no-op
                }
            }
            when (attachment) {
                is ProcessedAttachment.item -> {
                    val item = attachment.item
                    Timber.i("submit item")
                    submit(item = item, libraryId = libraryId, userId = userId)
                }

                is ProcessedAttachment.itemWithAttachment -> {
                    val item = attachment.item
                    val attachmentData = attachment.attachment
                    val attachmentFile = attachment.attachmentFile
                    Timber.i("Submit item with attachment")
                    val data = UploadData.init(
                        item = item,
                        attachmentKey = this.attachmentKey,
                        attachmentData = attachmentData,
                        attachmentFile = attachmentFile,
                        linkType = Attachment.FileLinkType.importedUrl,
                        defaultTitle = (viewState.title ?: "Unknown"),
                        libraryId = libraryId,
                        userId = userId,
                        dateParser = this.dateParser,
                        fileStore = this.fileStore
                    )
                    upload(data = data)
                }

                is ProcessedAttachment.file -> {
                    val file = attachment.file
                    val filename = attachment.fileName
                    Timber.i("Upload local file")
                    val data = UploadData.init(
                        file = file,
                        filename = filename,
                        attachmentKey = this.attachmentKey,
                        linkType = if (viewState.url == null) Attachment.FileLinkType.importedFile else Attachment.FileLinkType.importedUrl,
                        remoteUrl = viewState.url,
                        collections = collectionKeys,
                        tags = tags,
                        libraryId = libraryId,
                        userId = userId,
                        fileStore = this.fileStore
                    )
                    upload(data = data)
                }
            }

        } else if (viewState.url != null) {
            val url = viewState.url!!
            Timber.i("Submit webpage")

            val date = Date()
            val fields: Map<KeyBaseKeyPair, String> =
                mapOf(
                    KeyBaseKeyPair(key = FieldKeys.Item.Attachment.url, baseKey = null) to url,
                    KeyBaseKeyPair(key = FieldKeys.Item.title, baseKey = null) to (viewState.title
                        ?: "Unknown"),
                    KeyBaseKeyPair(
                        key = FieldKeys.Item.accessDate,
                        baseKey = null
                    ) to iso8601DateFormatV2.format(date)
                )

            val webItem = ItemResponse(
                rawType = ItemTypes.webpage,
                key = KeyGenerator.newKey(),
                library = LibraryResponse.init(libraryId = libraryId),
                parentKey = null,
                collectionKeys = collectionKeys,
                links = null,
                parsedDate = null,
                isTrash = false,
                version = 0,
                dateModified = date,
                dateAdded = date,
                fields = fields,
                tags = tags,
                creators = emptyList(),
                relations = JsonObject(),
                createdBy = null,
                lastModifiedBy = null,
                rects = null,
                paths = null,
                inPublications = false
            )

            submit(item = webItem, libraryId = libraryId, userId = userId)
        } else {
            Timber.i("Nothing to submit")
        }
    }


    private suspend fun submit(
        item: ItemResponse,
        libraryId: LibraryIdentifier,
        userId: Long,
    ) {
        try {
            val (parameters, changeUuids) = createItem(
                item,
                libraryId = libraryId,
                schemaController = schemaController,
                dateParser = dateParser
            )
            val result = SubmitUpdateSyncAction(
                parameters = listOf(parameters),
                changeUuids = changeUuids,
                sinceVersion = null,
                objectS = SyncObject.item,
                libraryId = libraryId,
                userId = userId,
                updateLibraryVersion = false
            ).result()
            if (result is CustomResult.GeneralSuccess) {
                triggerEffect(ShareViewEffect.NavigateBack)
            } else {
                result as CustomResult.GeneralError
                Timber.e("ShareViewModel: could not submit standalone item")
                updateState {
                    copy(
                        attachmentState = AttachmentState.failed(
                            attachmentError(
                                generalError = result,
                                libraryId = libraryId
                            )
                        ),
                        isSubmitting = false
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "ShareViewModel: could not submit standalone item")
            updateState {
                copy(
                    attachmentState = AttachmentState.failed(
                        attachmentError(
                            generalError = CustomResult.GeneralError.CodeError(e),
                            libraryId = libraryId
                        )
                    ),
                    isSubmitting = false
                )
            }
        }
    }

    private fun createItem(item: ItemResponse, libraryId: LibraryIdentifier, schemaController: SchemaController, dateParser: DateParser): Pair<Map<String, Any>, Map<String, List<String>>> {
        var changeUuids: MutableMap<String, List<String>> = mutableMapOf()
        var parameters: MutableMap<String, Any> = mutableMapOf()
        dbWrapper.realmDbStorage.perform { coordinator ->
            val collectionKey = item.collectionKeys.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = libraryId
                    )
                )
            }

            val request = CreateBackendItemDbRequest(item = item, schemaController = schemaController, dateParser = dateParser)
            val item = coordinator.perform(request = request)
            parameters = item.updateParameters?.toMutableMap() ?: mutableMapOf()
            changeUuids = mutableMapOf(item.key to item.changes.map{ it.identifier })

            coordinator.invalidate()
        }

        return parameters to changeUuids
    }

    private fun createItems(item: ItemResponse, attachment: Attachment): CreateItemsResult {
        Timber.i("ShareViewModel: create item and attachment db items")
        val parameters: MutableList<Map<String, Any>> = mutableListOf()
        var changeUuids: MutableMap<String, List<String>> = mutableMapOf()
        var mtime: Long? = null
        var md5: String? = null
        dbWrapper.realmDbStorage.perform {coordinator ->
            val collectionKey = item.collectionKeys.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = attachment.libraryId
                    )
                )
            }
            val request = CreateItemWithAttachmentDbRequest(
                item = item,
                attachment = attachment,
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                fileStore = this.fileStore
            )
            val(item, attachment) = coordinator.perform(request=  request)
            val itemUpdateParameters = item.updateParameters
            if (itemUpdateParameters != null) {
                parameters.add(itemUpdateParameters)
            }
            val updateParameters = attachment.updateParameters
            if (updateParameters != null){
                parameters.add(updateParameters)
            }
            changeUuids = mutableMapOf(item.key to item.changes.map{ it.identifier }, attachment.key to attachment.changes.map{ it.identifier })

            mtime = attachment.fields.where().key(FieldKeys.Item.Attachment.mtime).findFirst()?.value?.toLongOrNull()
            md5 = attachment.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst()?.value

            coordinator.invalidate()
        }
        if (mtime == null) {
            throw AttachmentState.Error.mtimeMissing
        }
        if (md5 == null) {
            throw AttachmentState.Error.md5Missing
        }
        return CreateItemsResult(parameters, changeUuids, md5!!, mtime!!)
    }



    private fun create(
        attachment: Attachment,
        collections: Set<String>,
        tags: List<TagResponse>
    ): CreateResult {
        Timber.i("Create attachment db item")

        val localizedType =
            this.schemaController.localizedItemType(itemType = ItemTypes.attachment) ?: ""

        var updateParameters: Map<String, Any>? = null
        var changeUuids: Map<String, List<String>>? = null
        var md5: String? = null
        var mtime: Long? = null

        dbWrapper.realmDbStorage.perform { coordinator ->
            val collectionKey = collections.firstOrNull()
            if (collectionKey != null) {
                coordinator.perform(
                    request = UpdateCollectionLastUsedDbRequest(
                        key = collectionKey,
                        libraryId = attachment.libraryId
                    )
                )
            }

            val request = CreateAttachmentDbRequest(
                attachment = attachment,
                parentKey = null,
                localizedType = localizedType,
                includeAccessDate = attachment.hasUrl,
                collections = collections,
                tags = tags,
                fileStore = fileStore
            )
            val attachment = coordinator.perform(request = request)

            updateParameters = attachment.updateParameters?.toMutableMap()
            changeUuids = mutableMapOf(attachment.key to attachment.changes.map { it.identifier })
            mtime = attachment.fields.where().key(FieldKeys.Item.Attachment.mtime)
                .findFirst()?.value?.toLongOrNull()
            md5 = attachment.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst()?.value

            coordinator.invalidate()
        }

        mtime ?: throw AttachmentState.Error.mtimeMissing
        md5 ?: throw AttachmentState.Error.md5Missing
        return CreateResult(
            updateParameters = updateParameters ?: emptyMap(),
            changeUuids = changeUuids ?: emptyMap(),
            md5 = md5!!,
            mtime = mtime!!
        )
    }

    private data class CreateResult(val updateParameters: Map<String, Any>, val changeUuids: Map<String, List<String>>, val md5: String, val mtime: Long)
    private data class CreateItemsResult(val parameters: List<Map<String, Any>>, val changeUuids: Map<String, List<String>>, val md5: String, val mtime: Long)

    private fun moveFile(fromFile: File, toFile: File): Long {
        Timber.i("ShareViewModel: move file to attachment folder")

        try {
            val size = fromFile.length()
            if (size == 0L) {
                throw AttachmentState.Error.fileMissing
            }
            if (fromFile.renameTo(toFile)) {
                return size
            } else {
                throw AttachmentState.Error.fileMissing
            }
        } catch (error: Exception) {
            Timber.e(error, "ShareViewModel: can't move file")
            fromFile.delete()
            throw error
        }
    }

    private suspend fun prepareAndSubmit(
        attachment: Attachment,
        collections: Set<String>,
        tags: List<TagResponse>,
        file: File,
        tmpFile: File,
        libraryId: LibraryIdentifier,
        userId: Long,
    ): CustomResult<ShareSubmissionData> {
        val filesize = moveFile(tmpFile, file)
        val data: ShareSubmissionData
        val parameters: Map<String, Any>
        val changeUuids: Map<String, List<String>>
        try {
            val (params, uuids, md5, mtime) = create(
                attachment = attachment,
                collections = collections,
                tags = tags
            )
            parameters = params
            changeUuids = uuids
            data = ShareSubmissionData(filesize = filesize, md5 = md5, mtime = mtime)
        } catch (e: Exception) {
            file.delete()
            return CustomResult.GeneralError.CodeError(e)
        }
        val result = SubmitUpdateSyncAction(
            parameters = listOf(parameters),
            changeUuids = changeUuids,
            sinceVersion = null,
            objectS = SyncObject.item,
            libraryId = libraryId,
            userId = userId,
            updateLibraryVersion = false
        ).result()
        if (result is CustomResult.GeneralSuccess) {
            return CustomResult.GeneralSuccess(data)
        }
        return result as CustomResult.GeneralError
    }

    private suspend fun prepareAndSubmit(
        item: ItemResponse,
        attachment: Attachment,
        file: File,
        tmpFile: File,
        libraryId: LibraryIdentifier,
        userId: Long,
    ): CustomResult<ShareSubmissionData> {
        val filesize = moveFile(tmpFile, file)
        val data: ShareSubmissionData
        val parameters: List<Map<String, Any>>
        val changeUuids: Map<String, List<String>>
        try {
            val (params, uuids, md5, mtime) = createItems(item = item, attachment = attachment)
            parameters = params
            changeUuids = uuids
            data = ShareSubmissionData(filesize = filesize, md5 = md5, mtime = mtime)
        } catch (e: Exception) {
            file.delete()
            return CustomResult.GeneralError.CodeError(e)
        }

        val result = SubmitUpdateSyncAction(
            parameters = parameters,
            changeUuids = changeUuids,
            sinceVersion = null,
            objectS = SyncObject.item,
            libraryId = libraryId,
            userId = userId,
            updateLibraryVersion = false
        ).result()
        if (result is CustomResult.GeneralSuccess) {
            return CustomResult.GeneralSuccess(data)
        }
        return result as CustomResult.GeneralError
    }

    private suspend fun submit(data: UploadData): CustomResult<ShareSubmissionData> {
        when (val type = data.type) {
            is UploadData.Kind.file -> {
                val location = type.location
                val collections = type.collections
                val tags = type.tags
                Timber.i("ShareViewModel: prepare upload for local file")
                return prepareAndSubmit(
                    attachment = data.attachment,
                    collections = collections,
                    tags = tags,
                    file = data.file,
                    tmpFile = location,
                    libraryId = data.libraryId,
                    userId = data.userId,
                )
            }

            is UploadData.Kind.translated -> {
                val item = type.item
                val location = type.location
                Timber.i("ShareViewModel: prepare upload for local file")
                return prepareAndSubmit(
                    item = item,
                    attachment = data.attachment,
                    file = data.file,
                    tmpFile = location,
                    libraryId = data.libraryId,
                    userId = data.userId
                )
            }
        }
    }

    private suspend fun upload(data: UploadData) {
        //TODO implement Upload to WebDav Support
        uploadToZotero(data = data)
    }

    private suspend fun uploadToZotero(data: UploadData) {
        try {
            val submissionDataResult = submit(data = data)
            if (submissionDataResult is CustomResult.GeneralError) {
                processUploadToZoteroException(submissionDataResult, data)
                return
            }
            val submissionData = (submissionDataResult as CustomResult.GeneralSuccess).value!!
            val uploadSyncResult = AuthorizeUploadSyncAction(
                key = data.attachment.key,
                filename = data.filename,
                filesize = submissionData.filesize,
                md5 = submissionData.md5,
                mtime = submissionData.mtime,
                libraryId = data.libraryId,
                userId = data.userId,
                oldMd5 = null
            ).result()
            if (uploadSyncResult is CustomResult.GeneralError) {
                processUploadToZoteroException(uploadSyncResult, data)
                return
            }
            uploadSyncResult as CustomResult.GeneralSuccess
            val response = uploadSyncResult.value!!
            val md5 = submissionData.md5
            when (response) {
                is AuthorizeUploadResponse.exists -> {
                    Timber.i("ShareViewModel: file exists remotely")
                    val request = MarkAttachmentUploadedDbRequest(
                        libraryId = data.libraryId,
                        key = data.attachment.key,
                        version = response.version
                    )
                    val request2 = UpdateVersionsDbRequest(
                        version = response.version,
                        libraryId = data.libraryId,
                        type = UpdateVersionType.objectS(SyncObject.item)
                    )
                    dbWrapper.realmDbStorage.perform(listOf(request, request2))
                }

                is AuthorizeUploadResponse.new -> {
                    val response = response.authorizeNewUploadResponse
                    Timber.i("ShareViewModel: upload authorized")

                    val upload = BackgroundUpload(
                        type = BackgroundUpload.Kind.zotero(uploadKey = response.uploadKey),
                        key = this.attachmentKey,
                        libraryId = data.libraryId,
                        userId = data.userId,
                        remoteUrl = response.url,
                        fileUrl = data.file,
                        md5 = md5,
                        date = Date()
                    )
                    backgroundUploadProcessor.startAsync(
                        upload = upload,
                        filename = data.filename,
                        mimeType = this.defaultMimetype,
                        parameters = response.params,
                        headers = mapOf("If-None-Match" to "*")
                    )

                }
            }
            triggerEffect(ShareViewEffect.NavigateBack)
        } catch (e: Exception) {
            Timber.e(e, "Could not submit item or attachment")
            processUploadToZoteroException(CustomResult.GeneralError.CodeError(e), data)
        }
    }

    private fun processUploadToZoteroException(
        error : CustomResult.GeneralError,
        data: UploadData
    ) {

        updateState {
            copy(
                attachmentState = AttachmentState.failed(
                    attachmentError(
                        error, libraryId = data.libraryId
                    )
                ),
                isSubmitting = false

            )
        }
    }

    fun errorMessage(error: AttachmentState.Error): String? {
        return when (error) {
            AttachmentState.Error.apiFailure -> {
                context.getString(Strings.errors_shareext_api_error)
            }
            AttachmentState.Error.cantLoadSchema -> {
                context.getString(Strings.errors_shareext_cant_load_schema)
            }
            AttachmentState.Error.cantLoadWebData -> {
                context.getString(Strings.errors_shareext_cant_load_data)
            }
            AttachmentState.Error.downloadFailed -> {
                context.getString(Strings.errors_shareext_download_failed)
            }
            AttachmentState.Error.downloadedFileNotPdf -> {
                null
            }
            AttachmentState.Error.expired -> {
                context.getString(Strings.errors_shareext_unknown)
            }
            AttachmentState.Error.fileMissing -> {
                context.getString(Strings.errors_shareext_missing_file)
            }
            AttachmentState.Error.itemsNotFound -> {
                context.getString(Strings.errors_shareext_items_not_found)
            }
            AttachmentState.Error.md5Missing -> {
                null
            }
            AttachmentState.Error.mtimeMissing -> {
                null
            }
            is AttachmentState.Error.parseError -> {
                context.getString(Strings.errors_shareext_parsing_error)
            }
            is AttachmentState.Error.quotaLimit -> {
                when (error.libraryIdentifier) {
                    is LibraryIdentifier.custom -> {
                        context.getString(Strings.errors_shareext_personal_quota_reached)
                    }
                    is LibraryIdentifier.group -> {
                        val groupId = error.libraryIdentifier.groupId
                        val group =
                            dbWrapper.realmDbStorage.perform(ReadGroupDbRequest(identifier = groupId))
                        val groupName = group?.name ?: "$groupId"
                        return context.getString(
                            Strings.errors_shareext_group_quota_reached,
                            groupName
                        )
                    }
                }
            }

            is AttachmentState.Error.schemaError -> {
                context.getString(Strings.errors_shareext_schema_error)
            }
            AttachmentState.Error.unknown -> {
                context.getString(Strings.errors_shareext_unknown)
            }
            AttachmentState.Error.webDavFailure -> {
                context.getString(Strings.errors_shareext_webdav_error)
            }
            AttachmentState.Error.webDavNotVerified -> {
                context.getString(Strings.errors_shareext_webdav_not_verified)
            }
            is AttachmentState.Error.webViewError -> {
                return when (error.error) {
                    TranslationWebViewError.cantFindFile -> {
                        context.getString(Strings.errors_shareext_missing_base_files)
                    }
                    TranslationWebViewError.incompatibleItem -> {
                        context.getString(Strings.errors_shareext_incompatible_item)
                    }
                    TranslationWebViewError.javascriptCallMissingResult -> {
                        context.getString(Strings.errors_shareext_javascript_failed)
                    }
                    TranslationWebViewError.noSuccessfulTranslators -> {
                        null
                    }
                    TranslationWebViewError.webExtractionMissingData -> {
                        context.getString(Strings.errors_shareext_response_missing_data)
                    }
                    TranslationWebViewError.webExtractionMissingJs -> {
                        context.getString(Strings.errors_shareext_missing_base_files)
                    }
                }
            }
        }
    }
}

internal data class ShareViewState(
    val title: String? = null,
    val url: String? = null,
    val attachmentState: AttachmentState = AttachmentState.decoding,
    val expectedItem: ItemResponse? = null,
    val expectedAttachment: Pair<String, File>? = null,
    val processedAttachment: ProcessedAttachment? = null,
    val collectionPickerState: CollectionPickerState = CollectionPickerState.loading,
    val recents: List<RecentData> = emptyList(),
    val itemPickerState: ItemPickerState? = null,
    val tags: List<Tag> = emptyList(),
    val isSubmitting: Boolean = false,
) : ViewState

internal sealed class ShareViewEffect : ViewEffect {
    object NavigateBack : ShareViewEffect()
    object NavigateToTagPickerScreen: ShareViewEffect()
    object NavigateToCollectionPickerScreen: ShareViewEffect()
}
