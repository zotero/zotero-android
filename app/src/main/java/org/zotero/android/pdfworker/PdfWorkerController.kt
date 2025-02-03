package org.zotero.android.pdfworker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.mappers.CreatorResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.TagResponseMapper
import org.zotero.android.api.pojo.sync.LibraryResponse
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
import org.zotero.android.pdfworker.data.PdfWorkerRecognizeError
import org.zotero.android.pdfworker.data.PdfWorkerRecognizedData
import org.zotero.android.pdfworker.web.PdfWorkerWebCallChainExecutor
import org.zotero.android.screens.addbyidentifier.IdentifierLookupController
import org.zotero.android.screens.addbyidentifier.IdentifierLookupController.LookupData
import org.zotero.android.screens.addbyidentifier.IdentifierLookupMode
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
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

    private lateinit var itemKey: String
    private lateinit var collections: List<String>

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
                processChainExecutorResult(result)
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
                if (state is LookupData.State.translatedForRecognizer) {
                    val createdItem = state.createdItem
                    updateItemAndPostProgress(createdItem, state.rawResponse)
                }
            }
        }

    }

    private fun updateItemAndPostProgress(
        createdItem: RItem,
        jsonObject: JsonObject,
    ) {
        dbWrapperMain.realmDbStorage.perform(
            LinkAttachmentToParentItemDbRequest(
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                libraryId = fileStore.getSelectedLibrary(),
                itemKey = this.itemKey,
                parentItemKey = createdItem.key
            )
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJsonString = gson.toJson(jsonObject)
        observable.emitAsync(Update.recognizedAndSaved(prettyJsonString))
    }

    private fun processChainExecutorResult(result: Result<PdfWorkerRecognizedData>) {
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
        when (successValue) {
            PdfWorkerRecognizedData.recognizedDataIsEmpty -> {
                observable.emitAsync(Update.recognizedDataIsEmpty)
            }

            is PdfWorkerRecognizedData.fallbackItem -> {
                val itemData = successValue.rawData
                val libraryId = fileStore.getSelectedLibrary()

                val item = itemResponseMapper.fromTranslatorResponse(
                    response = itemData,
                    schemaController = schemaController,
                    tagResponseMapper = tagResponseMapper,
                    creatorResponseMapper = creatorResponseMapper
                ).copy(
                    collectionKeys = this.collections.toSet(),
                    library = LibraryResponse.init(libraryId)
                )


                val createdItem = dbWrapperMain.realmDbStorage.perform(
                    CreateTranslatedItemsDbRequest(
                        responses = listOf(
                            item
                        ),
                        schemaController = schemaController,
                        dateParser = dateParser
                    )
                )[0]
                updateItemAndPostProgress(createdItem = createdItem, jsonObject = itemData)
            }

            is PdfWorkerRecognizedData.itemWithIdentifier -> {
                val libraryId = fileStore.getSelectedLibrary()
                val collectionKeys =
                    fileStore.getSelectedCollectionId().keyGet?.let { setOf(it) } ?: emptySet()
                mainCoroutineScope.launch {
                    identifierLookupController.setRecognizedData(successValue.item)
                    identifierLookupController.lookUp(
                        libraryId = libraryId,
                        collectionKeys = collectionKeys,
                        identifier = successValue.identifier
                    )
                }

            }
        }

    }

    fun recognize(itemKey: String, libraryId: LibraryIdentifier) {
        this.itemKey = itemKey
        val request = ReadItemDbRequest(libraryId = libraryId, key = itemKey)
        val item = dbWrapperMain.realmDbStorage.perform(request = request)
        this.collections = item.collections!!.map { it.key }

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

        observable.emitAsync(Update.recognizeInit(fileName = filename))

        val collectionKeys =
            fileStore.getSelectedCollectionId().keyGet?.let { setOf(it) } ?: emptySet()

        identifierLookupController.initialize(
            lookupMode = IdentifierLookupMode.recognize,
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

    fun cancelAllLookups() {
        identifierLookupController.cancelAllLookups()
    }

    sealed interface Update {
        data class recognizeInit(val fileName: String) : Update
        object recognizedDataIsEmpty: Update
        data class recognizeError(val errorMessage: String) : Update
        data class recognizedAndSaved(val result: String) : Update
    }
}