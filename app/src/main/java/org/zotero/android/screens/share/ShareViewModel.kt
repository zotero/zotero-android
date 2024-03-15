package org.zotero.android.screens.share

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
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
import org.zotero.android.androidx.content.longToast
import org.zotero.android.api.mappers.CreatorResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.TagResponseMapper
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadCollectionAndLibraryDbRequest
import org.zotero.android.database.requests.ReadRecentCollections
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.screens.collectionpicker.data.CollectionPickerSingleResult
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.ItemPickerState
import org.zotero.android.screens.share.data.ProcessedAttachment
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SchemaError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObservableEventStream
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.sync.Tag
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.translator.data.TranslatorAction
import org.zotero.android.translator.data.TranslatorActionEventStream
import org.zotero.android.translator.web.TranslatorWebCallChainExecutor
import org.zotero.android.translator.web.TranslatorWebExtractionExecutor
import timber.log.Timber
import java.io.File
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
    fun onEvent(result: CollectionPickerSingleResult) {
       //TODO
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
            Timber.e(error, "ExtensionViewModel: could not process item ")
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
           //TODO download
        context.longToast("Download of an attachment not implemented yet")
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
                    url = generalError.httpUrl?.toUrl()?.toString(),
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
        context.longToast("Not Implemented Yet")
//        ScreenArguments.collectionPickerArgs = CollectionPickerArgs(
//            mode = CollectionPickerMode.single(title = ""),
//            libraryId = this.selectedLibraryId,
//            excludedKeys = emptySet(),
//            selected = emptySet()
//        )
//        triggerEffect(ShareViewEffect.NavigateToCollectionPickerScreen)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun set(collection: Collection?) {

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
) : ViewState

internal sealed class ShareViewEffect : ViewEffect {
    object NavigateBack : ShareViewEffect()
    object NavigateToTagPickerScreen: ShareViewEffect()
    object NavigateToCollectionPickerScreen: ShareViewEffect()
}
