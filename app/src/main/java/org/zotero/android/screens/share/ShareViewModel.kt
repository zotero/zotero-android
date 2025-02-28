package org.zotero.android.screens.share

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadCollectionAndLibraryDbRequest
import org.zotero.android.database.requests.ReadRecentCollections
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.pdfworker.PdfWorkerController
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.ItemPickerState
import org.zotero.android.screens.share.data.ProcessedAttachment
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.screens.share.data.UploadData
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerArgs
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerResults
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.SyncObservableEventStream
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.sync.Tag
import org.zotero.android.sync.syncactions.SubmitUpdateSyncAction
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslatorAction
import org.zotero.android.translator.data.TranslatorActionEventStream
import org.zotero.android.translator.web.TranslatorWebCallChainExecutor
import org.zotero.android.translator.web.TranslatorWebExtractionExecutor
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
    private val dbWrapperMain: DbWrapperMain,
    private val itemResponseMapper: ItemResponseMapper,
    private val schemaController: SchemaController,
    private val tagResponseMapper: TagResponseMapper,
    private val creatorResponseMapper: CreatorResponseMapper,
    private val defaults: Defaults,
    private val dateParser: DateParser,
    private val shareFileDownloader: ShareFileDownloader,
    private val shareErrorProcessor: ShareErrorProcessor,
    private val shareItemSubmitter: ShareItemSubmitter,
    private val pdfWorkerController: PdfWorkerController,
) : BaseViewModel2<ShareViewState, ShareViewEffect>(ShareViewState()) {

    private val defaultLibraryId: LibraryIdentifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
    private val defaultExtension = "pdf"
    private val defaultMimetype = "application/pdf"
    private val zipMimetype = "application/zip"

    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var selectedCollectionId: CollectionIdentifier
    private lateinit var selectedLibraryId: LibraryIdentifier
    private lateinit var attachmentKey: String
    private var wasAttachmentUploaded: Boolean = false

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
                Timber.i("ShareViewModel: start async collections sync")
                syncScheduler.startSyncController(
                    type = SyncKind.collectionsOnly,
                    libraries = Libraries.all,
                    retryAttempt = 0
                )
                val attachment = shareRawAttachmentLoader.getLoadedAttachmentResult()
                process(attachment)
            } catch (e: Exception) {
                Timber.e(e, "ShareViewModel: could not load attachment")
                updateAttachmentState(
                    AttachmentState.failed(
                        shareErrorProcessor.attachmentError(
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
                                shareErrorProcessor.attachmentError(
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
        val attachment: JsonObject?

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
                        shareErrorProcessor.attachmentError(
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
        val url = attachment["url"].asString
        Timber.i("ShareViewModel: parsed item with attachment, download attachment")

        val file =
            fileStore.shareExtensionDownload(key = this.attachmentKey, ext = this.defaultExtension)
        download(
            item = item,
            attachment = attachment,
            url = url,
            file = file,
            cookies = cookies,
            userAgent = userAgent,
            referrer = referrer
        )
    }

    private fun download(
        item: ItemResponse,
        attachment: JsonObject,
        url: String,
        file: File,
        cookies: String?,
        userAgent: String?,
        referrer: String?
    ) {
        val attachmentTitle = ((attachment["title"]?.asString) ?: viewState.title) ?: ""

        updateState {
            copy(
                attachmentState = AttachmentState.downloading(0),
                expectedItem = item,
                expectedAttachment = attachmentTitle to file,
                processedAttachment = ProcessedAttachment.item(item)
            )
        }

        ioCoroutineScope.launch {
            try {
                shareFileDownloader.download(
                    url = url,
                    file = file,
                    cookies = cookies,
                    userAgent = userAgent,
                    referrer = referrer,
                    updateProgressBar = ::updateProgressBar,
                )
                processDownload(
                    attachment = attachment,
                    url = url,
                    file = file,
                    item = item,
                    cookies = cookies,
                    userAgent = userAgent,
                    referrer = referrer
                )
            } catch (e: Exception) {
                Timber.e("ShareViewModel: could not download translated file - $url")
                viewModelScope.launch {
                    updateState {
                        copy(attachmentState = AttachmentState.failed(AttachmentState.Error.downloadFailed))
                    }
                }
            }
        }
    }

    private fun processDownload(
        attachment: JsonObject,
        url: String,
        file: File,
        item: ItemResponse,
        cookies: String?,
        userAgent: String?,
        referrer: String?
    ) {
        if (fileStore.isPdf(file = file)) {
            Timber.i("ShareViewModel: downloaded pdf")
            viewModelScope.launch {
                updateState {
                    copy(
                        attachmentState = AttachmentState.processed,
                        processedAttachment = ProcessedAttachment.itemWithAttachment(
                            item = item,
                            attachment = attachment,
                            attachmentFile = file
                        )
                    )
                }
            }
            return
        }
        Timber.i("ShareViewModel: downloaded unsupported attachment")

        file.delete()
        viewModelScope.launch {
            updateState {
                copy(attachmentState = AttachmentState.failed(AttachmentState.Error.downloadedFileNotPdf))
            }
        }

    }

    private fun finishSync(successful: Boolean) {
        Timber.i("ShareViewModel: finishSync success = $successful")
        if (!successful && wasAttachmentUploaded) {
            updateState {
                copy(collectionPickerState = CollectionPickerState.failed)
            }
            return
        }


        try {
            var library: Library? = null
            var collection: Collection? = null
            var recents = mutableListOf<RecentData>()
            Timber.i("ShareViewModel: ReadCollectionAndLibraryDbRequest closure before")
            dbWrapperMain.realmDbStorage.perform { coordinator ->
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
                Timber.i("ShareViewModel: ReadCollectionAndLibraryDbRequest closure exec finish. recents = ${recents.size}, collection = ${collection}, library = $_library")
            }
            Timber.i("ShareViewModel: ReadCollectionAndLibraryDbRequest closure after. recents = ${recents.size}, collection = ${collection}, library = $library")
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
            Timber.e(e, "ShareViewModel: can't load collections")
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
                    shareErrorProcessor.attachmentError(
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
                reportFileIsNotPdf()
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
                reportFileIsNotPdf()
                process(url = attachment.url)
            }
            is RawAttachment.fileUrl -> {
                process(uri = attachment.uri)
            }

            is RawAttachment.remoteFileUrl -> {
                process(
                    url = attachment.url,
                    contentType = attachment.contentType,
                    cookies = attachment.cookies,
                    userAgent = attachment.userAgent,
                    referrer = attachment.referrer
                )
            }
        }
    }

    private suspend fun process(
        url: String,
        contentType: String?,
        cookies: String?,
        userAgent: String?,
        referrer: String?
    ) {
        val filename = url.toUri().lastPathSegment!!
        val ext = MimeTypeMap.getFileExtensionFromUrl(filename)
        val file = fileStore.shareExtensionDownload(key = this.attachmentKey, ext = ext)

        viewModelScope.launch {
            updateState {
                copy(
                    url = url,
                    title = url,
                    attachmentState = AttachmentState.downloading(0),
                    expectedAttachment = filename to file
                )
            }

        }
        try {
            shareFileDownloader.download(
                url = url,
                file = file,
                cookies = cookies,
                userAgent = userAgent,
                referrer = referrer,
                updateProgressBar = ::updateProgressBar,
            )
            viewModelScope.launch {

                if (fileStore.isPdf(file)) {
                    Timber.i("ShareViewModel: downloaded pdf")

                    updateState {
                        copy(
                            processedAttachment = ProcessedAttachment.file(
                                file = file,
                                fileName = filename
                            ),
                            attachmentState = AttachmentState.processed
                        )
                    }
                    attemptToRecognize(tmpFile = file, fileName = filename)
                } else {
                    Timber.i("ShareViewModel: downloaded unsupported file")
                    updateState {
                        copy(
                            processedAttachment = null,
                            attachmentState = AttachmentState.failed(
                                AttachmentState.Error.downloadedFileNotPdf
                            )
                        )
                    }
                    file.delete()
                    reportFileIsNotPdf()
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Could not download shared file - $url")
            viewModelScope.launch {
                updateState {
                    copy(attachmentState = AttachmentState.failed(AttachmentState.Error.downloadFailed))
                }
            }

            file.delete()
        }
    }

    private fun updateProgressBar(progress: Int) {
        viewModelScope.launch {
            if (viewState.attachmentState is AttachmentState.downloading) {
                updateState {
                    copy(attachmentState = AttachmentState.downloading(progress))
                }
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
                attemptToRecognize(tmpFile = tmpFile, fileName = fileName)
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
        ioCoroutineScope.cancel()
        pdfWorkerController.cancelAllLookups()
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
            val (parameters, changeUuids) = shareItemSubmitter.createItem(
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
                            shareErrorProcessor.attachmentError(
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
                        shareErrorProcessor.attachmentError(
                            generalError = CustomResult.GeneralError.CodeError(e),
                            libraryId = libraryId
                        )
                    ),
                    isSubmitting = false
                )
            }
        }
    }


    private suspend fun upload(data: UploadData) {
        if (defaults.isWebDavEnabled()) {
            shareItemSubmitter.uploadToWebDav(
                data = data,
                attachmentKey = this.attachmentKey,
                zipMimetype = this.zipMimetype,
                processUploadToZoteroException = ::processUploadToZoteroException,
                onBack = {
                    wasAttachmentUploaded = true
                    maybeSaveCachedDataInPdfWorker()
                })
        } else {
            shareItemSubmitter.uploadToZotero(
                data = data,
                attachmentKey = this.attachmentKey,
                defaultMimetype = this.defaultMimetype,
                processUploadToZoteroException = ::processUploadToZoteroException,
                onBack = {
                    wasAttachmentUploaded = true
                    maybeSaveCachedDataInPdfWorker()
                }
            )
        }

    }

    private fun maybeSaveCachedDataInPdfWorker() {
        if (viewState.retrieveMetadataState is RetrieveMetadataState.success) {
            val tags = viewState.tags.map { TagResponse(tag = it.name, type = it.type) }
            val collectionKeys = this.selectedCollectionId.keyGet?.let { setOf(it) } ?: emptySet()
            pdfWorkerController.saveCachedData(
                attachmentItemKey = this.attachmentKey,
                libraryId = this.selectedLibraryId,
                collectionKeys = collectionKeys,
                tags = tags
            )
        } else {
            if (wasAttachmentUploaded) {
                triggerEffect(ShareViewEffect.NavigateBack)
            }
        }


    }


    private fun processUploadToZoteroException(
        error: CustomResult.GeneralError,
        data: UploadData
    ) {
        wasAttachmentUploaded = false
        maybeSaveCachedDataInPdfWorker()
        updateState {
            copy(
                attachmentState = AttachmentState.failed(
                    shareErrorProcessor.attachmentError(
                        error, libraryId = data.libraryId
                    )
                ),
                isSubmitting = false

            )
        }
    }

    fun errorMessage(error: AttachmentState.Error): String? {
        return shareErrorProcessor.errorMessage(error)
    }

    private fun attemptToRecognize(tmpFile: File, fileName: String) {
        if (!fileStore.isPdf(tmpFile)) {
            reportFileIsNotPdf()
            return
        }

        pdfWorkerController.observable.flow()
            .onEach { result ->
                observe(result)
            }
            .launchIn(viewModelScope)

        pdfWorkerController.recognizeNewDocument(tmpFile = tmpFile, pdfFileName = fileName)
    }

    private fun reportFileIsNotPdf() {
        viewModelScope.launch {
            updateState {
                copy(retrieveMetadataState = RetrieveMetadataState.fileIsNotPdf)
            }
        }
    }

    private fun observe(update: PdfWorkerController.Update) {
        when(update) {
            is PdfWorkerController.Update.recognizeInit -> {
                //no-op
            }
            PdfWorkerController.Update.recognizedDataIsEmpty -> {
                updateState {
                    copy(retrieveMetadataState = RetrieveMetadataState.recognizedDataIsEmpty)
                }
            }
            is PdfWorkerController.Update.recognizeError -> {
                updateState { copy(retrieveMetadataState = RetrieveMetadataState.failed(update.errorMessage)) }
            }
            is PdfWorkerController.Update.recognizedAndSaved -> {
                if (wasAttachmentUploaded) {
                    triggerEffect(ShareViewEffect.NavigateBack)
                }
            }
            is PdfWorkerController.Update.recognizedAndKeptInMemory -> {
                updateState {
                    copy(retrieveMetadataState = RetrieveMetadataState.success(
                        recognizedTitle = update.recognizedTitle,
                        recognizedTypeIcon = update.recognizedTypeIcon
                    ))
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
    val retrieveMetadataState: RetrieveMetadataState = RetrieveMetadataState.loading,
) : ViewState

internal sealed class ShareViewEffect : ViewEffect {
    object NavigateBack : ShareViewEffect()
    object NavigateToTagPickerScreen: ShareViewEffect()
    object NavigateToCollectionPickerScreen: ShareViewEffect()
}
