package org.zotero.android.pdfworker

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.mappers.CreatorResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.TagResponseMapper
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.LibraryResponse
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.CreateTranslatedItemsDbRequest
import org.zotero.android.database.requests.LinkAttachmentToParentItemDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.pdfworker.data.PdfWorkerMode
import org.zotero.android.pdfworker.data.PdfWorkerRecognizeError
import org.zotero.android.pdfworker.data.PdfWorkerRecognizedData
import org.zotero.android.pdfworker.web.PdfWorkerWebCallChainExecutor
import org.zotero.android.screens.addbyidentifier.IdentifierLookupController
import org.zotero.android.screens.addbyidentifier.IdentifierLookupController.LookupData
import org.zotero.android.screens.addbyidentifier.data.IdentifierLookupMode
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfWorkerController @Inject constructor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
    private val itemResponseMapper: ItemResponseMapper,
    private val tagResponseMapper: TagResponseMapper,
    private val creatorResponseMapper: CreatorResponseMapper,
    private val dateParser: DateParser,
    private val schemaController: SchemaController,
    private val dbWrapperMain: DbWrapperMain,
    private val defaults: Defaults,
    private val identifierLookupController: IdentifierLookupController
) {

    private val mainCoroutineScope = CoroutineScope(dispatchers.main)
    lateinit var observable: EventStream<Update>
    private var pdfWorkerWebCallChainExecutor: PdfWorkerWebCallChainExecutor? = null

    private var pdfWorkerMode: PdfWorkerMode = PdfWorkerMode.recognizeAndWait

    private var itemResponse: ItemResponse? = null

    init {
        initialize()
    }

    private fun initialize() {
        observable = EventStream<Update>(
            ZoteroApplication.instance.applicationScope
        )

        pdfWorkerWebCallChainExecutor = PdfWorkerWebCallChainExecutor(
            context = this.context,
            dispatchers = dispatchers,
            gson = gson,
            fileStore = fileStore,
        )

        pdfWorkerWebCallChainExecutor?.observable?.flow()
            ?.onEach { result ->
                processPdfWorkerUpdate(result)
            }
            ?.launchIn(mainCoroutineScope)
    }

    private fun processIdentifierLookupResult(update: IdentifierLookupController.Update) {
        when (update.kind) {
            is IdentifierLookupController.Update.Kind.lookupError -> {
                observable.emitAsync(Update.recognizeError(update.kind.error.message ?: "Unknown Error"))
            }
            is IdentifierLookupController.Update.Kind.lookupFailed -> {
                observable.emitAsync(Update.recognizeError("Lookup Failed"))
            }

            is IdentifierLookupController.Update.Kind.itemCreationFailed -> {
                observable.emitAsync(Update.recognizeError("Item Creation Failed"))
            }

            is IdentifierLookupController.Update.Kind.parseFailed -> {
                observable.emitAsync(Update.recognizeError("Parsing Failed"))
            }

            else -> {
                val lookupData = update.lookupData.getOrNull(0) ?: return
                val state = lookupData.state
                if (state is LookupData.State.translatedAndCreatedItem) {
                    val createdItem = state.createdItem
                    val mode = pdfWorkerMode as PdfWorkerMode.recognizeAndSave
                    updateItemAndPostProgress(
                        createdItem = createdItem,
                        itemKey = mode.itemKey,
                        libraryIdentifier = mode.libraryIdentifier
                    )
                }
                if (state is LookupData.State.translatedOnly) {
                    this.itemResponse = state.itemResponse
                    observable.emitAsync(Update.recognizedAndKeptInMemory)
                }
            }
        }
    }

    private fun updateItemAndPostProgress(
        createdItem: RItem,
        itemKey: String,
        libraryIdentifier: LibraryIdentifier,
    ) {
        dbWrapperMain.realmDbStorage.perform(
            LinkAttachmentToParentItemDbRequest(
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                libraryId = libraryIdentifier,
                itemKey = itemKey,
                parentItemKey = createdItem.key
            )
        )

        observable.emitAsync(Update.recognizedAndSaved(createdItem.displayTitle))
    }

    private fun processPdfWorkerUpdate(result: Result<PdfWorkerRecognizedData>) {
        if (result is Result.Failure) {
            val customException = (result.exception as? PdfWorkerRecognizeError)?: return
            val errorMessage = when (customException) {
                PdfWorkerRecognizeError.failedToInitializePdfWorker -> {
                    Timber.e("PdfWorkerController: Pdf Worker's JS failed to initialize")
                    context.getString(Strings.retrieve_metadata_error_failed_to_initialize)
                }
                is PdfWorkerRecognizeError.recognizeFailed -> {
                    Timber.e("PdfWorkerController: recognizeFailed: ${customException.errorMessage}")
                    customException.errorMessage
                }
            }
            observable.emitAsync(Update.recognizeError(errorMessage))
            return
        }
        val successValue = (result as Result.Success).value
        val pdfWorkerMode = pdfWorkerMode
        when (successValue) {
            PdfWorkerRecognizedData.recognizedDataIsEmpty -> {
                observable.emitAsync(Update.recognizedDataIsEmpty)
            }

            is PdfWorkerRecognizedData.fallbackItem -> {
                val itemData = successValue.rawData

                var item = itemResponseMapper.fromTranslatorResponse(
                    response = itemData,
                    schemaController = schemaController,
                    tagResponseMapper = tagResponseMapper,
                    creatorResponseMapper = creatorResponseMapper
                )

                when(pdfWorkerMode) {
                    is PdfWorkerMode.recognizeAndSave -> {
                        val item = item.copy(
                            collectionKeys = pdfWorkerMode.collections,
                            library = LibraryResponse.init(pdfWorkerMode.libraryIdentifier)
                        )

                        saveItemWithAttachment(
                            item = item,
                            libraryIdentifier = pdfWorkerMode.libraryIdentifier,
                            itemKey = pdfWorkerMode.itemKey
                        )
                    }
                    PdfWorkerMode.recognizeAndWait -> {
                        this.itemResponse = item
                        observable.emitAsync(Update.recognizedAndKeptInMemory)
                    }
                }
            }

            is PdfWorkerRecognizedData.itemWithIdentifier -> {
                mainCoroutineScope.launch {
                    when (pdfWorkerMode) {
                        is PdfWorkerMode.recognizeAndSave -> {
                            identifierLookupController.setRecognizedData(successValue.item)
                            identifierLookupController.lookUp(
                                libraryId = pdfWorkerMode.libraryIdentifier,
                                collectionKeys = pdfWorkerMode.collections,
                                identifier = successValue.identifier
                            )

                        }

                        PdfWorkerMode.recognizeAndWait -> {
                            identifierLookupController.setRecognizedData(successValue.item)
                            identifierLookupController.lookUp(
                                libraryId = LibraryIdentifier.group(0),
                                collectionKeys = emptySet(),
                                identifier = successValue.identifier
                            )
                        }
                    }
                }
            }
        }
    }

    private fun saveItemWithAttachment(
        item: ItemResponse,
        libraryIdentifier: LibraryIdentifier,
        itemKey: String,
    ) {
        val createdItem = dbWrapperMain.realmDbStorage.perform(
            CreateTranslatedItemsDbRequest(
                responses = listOf(
                    item
                ),
                schemaController = schemaController,
                dateParser = dateParser
            )
        )[0]
        updateItemAndPostProgress(
            createdItem = createdItem,
            itemKey = itemKey,
            libraryIdentifier = libraryIdentifier
        )
    }

    fun recognizeExistingItem(itemKey: String, libraryId: LibraryIdentifier) {
        val request = ReadItemDbRequest(libraryId = libraryId, key = itemKey)
        val item = dbWrapperMain.realmDbStorage.perform(request = request)
        val collectionKeys = item.collections!!.map { it.key }.toSet()
        val libraryIdentifier = fileStore.getSelectedLibrary()
        this.pdfWorkerMode = PdfWorkerMode.recognizeAndSave(
            itemKey = itemKey,
            libraryIdentifier = libraryIdentifier,
            collections = collectionKeys
        )

        val attachment = AttachmentCreator.mainAttachment(
            item = item,
            fileStorage = this.fileStore,
            defaults = this.defaults
        )!!
        val filename = (attachment.type as Attachment.Kind.file).filename

        val attachmentFile = fileStore.attachmentFile(
            libraryId = libraryId,
            key = itemKey,
            filename = filename,
        )

        observable.emitAsync(Update.recognizeInit(pdfFileName = filename))

        identifierLookupController.initialize(
            lookupMode = IdentifierLookupMode.identifyAndSaveParentItem,
            libraryId = libraryId,
            collectionKeys = collectionKeys,
        ) { lookupData ->
            if (lookupData == null) {
                Timber.e("PdfWorkerController: can't create observer")
                return@initialize
            }
            identifierLookupController.observable
                .flow()
                .onEach { update ->
                    processIdentifierLookupResult(update)

                }.launchIn(mainCoroutineScope)
        }

        if (!attachmentFile.exists()) {
            observable.emitAsync(Update.recognizeError("File not found"))
            return
        }
        val pdfFilePath = "file://" + attachmentFile.absolutePath
        val pdfFileName = item.displayTitle
        pdfWorkerWebCallChainExecutor?.start(pdfFilePath = pdfFilePath, pdfFileName = pdfFileName)
    }

    fun recognizeNewDocument(tmpFile: File, pdfFileName: String) {
        pdfWorkerMode = PdfWorkerMode.recognizeAndWait
        identifierLookupController.initialize(
            lookupMode = IdentifierLookupMode.identifyOnly,
            libraryId = LibraryIdentifier.group(0),
            collectionKeys = emptySet(),
        ) { lookupData ->
            if (lookupData == null) {
                Timber.e("PdfWorkerController: can't create observer")
                return@initialize
            }
            identifierLookupController.observable
                .flow()
                .onEach { update ->
                    processIdentifierLookupResult(update)

                }.launchIn(mainCoroutineScope)
        }

        val pdfFilePath = "file://" + tmpFile.absolutePath
        pdfWorkerWebCallChainExecutor?.start(pdfFilePath = pdfFilePath, pdfFileName = pdfFileName)
    }

    fun saveCachedData(attachmentItemKey: String, libraryId: LibraryIdentifier, collectionKeys: Set<String>, tags: List<TagResponse>) {
        val itemResponse = this.itemResponse
        if (itemResponse == null) {
            return
        }
        val updatedItemResponse = itemResponse.copy(
            libraryId = libraryId,
            collectionKeys = collectionKeys,
            tags = tags + itemResponse.tags
        )


        saveItemWithAttachment(
            item = updatedItemResponse,
            libraryIdentifier = libraryId,
            itemKey = attachmentItemKey
        )

    }

    fun cancelAllLookups() {
        identifierLookupController.cancelAllLookups()
    }

    sealed interface Update {
        data class recognizeInit(val pdfFileName: String) : Update
        object recognizedDataIsEmpty: Update
        data class recognizeError(val errorMessage: String) : Update
        data class recognizedAndSaved(val recognizedTitle: String) : Update
        object recognizedAndKeptInMemory : Update
    }
}