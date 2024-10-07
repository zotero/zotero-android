package org.zotero.android.screens.addbyidentifier

import android.content.Context
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.mappers.CreatorResponseMapper
import org.zotero.android.api.mappers.ItemResponseMapper
import org.zotero.android.api.mappers.TagResponseMapper
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloader
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloaderEventStream
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.requests.CreateAttachmentDbRequest
import org.zotero.android.database.requests.CreateTranslatedItemsDbRequest
import org.zotero.android.database.requests.MarkItemsAsTrashedDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.ktx.unmarshalMap
import org.zotero.android.screens.addbyidentifier.data.LookupData
import org.zotero.android.screens.addbyidentifier.data.LookupSettings
import org.zotero.android.screens.share.data.FilenameFormatter
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.translator.loader.TranslatorsLoader
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentifierLookupController @Inject constructor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val translatorsLoader: TranslatorsLoader,
    private val fileStore: FileStore,
    private val nonZoteroApi: NonZoteroApi,
    private val remoteFileDownloader: RemoteAttachmentDownloader,
    private val itemResponseMapper: ItemResponseMapper,
    private val tagResponseMapper: TagResponseMapper,
    private val creatorResponseMapper: CreatorResponseMapper,
    private val dateParser: DateParser,
    private val schemaController: SchemaController,
    private val dbWrapperMain: DbWrapperMain,
    private val defaults: Defaults,
    private val translatorLoadedEventStream: TranslatorLoadedEventStream,
    private val attachmentDownloaderEventStream: RemoteAttachmentDownloaderEventStream,
) {

    private var lookupData: MutableMap<String, LookupData> = mutableMapOf()
    private var lookupSavedCount = 0
    private var lookupFailedCount = 0
    private val lookupTotalCount: Int
        get() {
            return lookupData.size
        }
    private val lookupRemainingCount: Int
        get() {
            return lookupTotalCount - lookupSavedCount - lookupFailedCount
        }

    private val lookupWebViewHandlersByLookupSettings: MutableMap<LookupSettings, LookupWebCallChainExecutor> =
        mutableMapOf()

    private val mainCoroutineScope = CoroutineScope(dispatchers.main)

    lateinit var observable: EventStream<Update>

    init {
        setupObservers()
    }

    private var shouldSkipLookupsCleaning = false

    fun initialize(
        libraryId: LibraryIdentifier,
        collectionKeys: Set<String>,
        shouldSkipLookupsCleaning: Boolean = false,
        completion: (List<LookupData>?) -> Unit
    ) {
        this.shouldSkipLookupsCleaning = shouldSkipLookupsCleaning
        observable = EventStream<Update>(
            ZoteroApplication.instance.applicationScope
        )
        val lookupSettings =
            LookupSettings(libraryIdentifier = libraryId, collectionKeys = collectionKeys)
        if (lookupWebViewHandlersByLookupSettings[lookupSettings] != null) {
            val lookupData = this.lookupData.values.toList()
            completion(lookupData)
            return
        }


        val lookupWebViewHandler = LookupWebCallChainExecutor(
            lookupSettings = lookupSettings,
            context = this.context,
            dispatchers = dispatchers,
            gson = gson,
            translatorsLoader = translatorsLoader,
            fileStore = fileStore,
            nonZoteroApi = nonZoteroApi,
            translatorLoadedEventStream = translatorLoadedEventStream,
        )
        lookupWebViewHandlersByLookupSettings[lookupSettings] = lookupWebViewHandler
        setupObserver(lookupWebViewHandler)
        val lookupData = this.lookupData.values.toList()
        completion(lookupData)
    }


    private fun setupObservers() {
        fun finish(download: RemoteAttachmentDownloader.Download, attachment: Attachment) {
            val localizedType =
                schemaController.localizedItemType(ItemTypes.attachment) ?: ItemTypes.attachment
            try {
                val request = CreateAttachmentDbRequest(
                    attachment = attachment,
                    parentKey = download.parentKey,
                    localizedType = localizedType,
                    includeAccessDate = attachment.hasUrl,
                    collections = emptySet(),
                    tags = emptyList(),
                    fileStore = this.fileStore,
                )
                dbWrapperMain.realmDbStorage.perform(request = request)
            } catch (error: Exception) {
                Timber.e(error, "IdentifierLookupController: can't store attachment after download")
                val (filename) = attachment.type as? Attachment.Kind.file ?: return
                val file = fileStore.attachmentFile(
                    attachment.libraryId,
                    key = attachment.key,
                    filename = filename
                )
                file.delete()
            }
        }

        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                var cleanupLookupIfNeeded = false
                when (update.kind) {
                    is RemoteAttachmentDownloader.Update.Kind.ready -> {
                        finish(download = update.download, attachment = update.kind.attachment)
                        cleanupLookupIfNeeded = true
                    }

                    RemoteAttachmentDownloader.Update.Kind.cancelled, RemoteAttachmentDownloader.Update.Kind.failed -> {
                        cleanupLookupIfNeeded = true
                    }

                    is RemoteAttachmentDownloader.Update.Kind.progress -> {
                        //no-op
                    }
                }
                if (!cleanupLookupIfNeeded) {
                    return@onEach
                }
                if (shouldSkipLookupsCleaning) {
                    return@onEach
                }
                cleanupLookupIfNeeded(force = false) {
                    observable.emitAsync(
                        Update(
                            kind = Update.Kind.finishedAllLookups,
                            lookupData = emptyList()
                        )
                    )
                }
            }.launchIn(mainCoroutineScope)

    }

    private fun setupObserver(lookupWebViewHandler: LookupWebCallChainExecutor) {

        fun identifier(data: Map<String, String>): String {
            var result = ""
            for ((key, value) in data) {
                result += key + ":" + value
            }
            return result
        }

        fun parse(
            itemData: JsonObject,
            libraryId: LibraryIdentifier,
            collectionKeys: Set<String>,
            schemaController: SchemaController,
            dateParser: DateParser,
        ): Pair<ItemResponse, List<Pair<Attachment, String>>>? {
            try {
                val item = itemResponseMapper.fromTranslatorResponse(
                    response = itemData,
                    schemaController = schemaController,
                    tagResponseMapper = tagResponseMapper,
                    creatorResponseMapper = creatorResponseMapper
                ).copy(libraryId = libraryId, collectionKeys = collectionKeys, tags = emptyList())
                val attachments = itemData["attachments"]?.asJsonArray?.mapNotNull {
                    val data = it.asJsonObject
                    val mimeType = data["mimeType"]?.asString ?: return@mapNotNull null
                    if (mimeType == "text/html") {
                        return@mapNotNull null
                    }
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                        ?: return@mapNotNull null
                    val url = data["url"]?.asString ?: return@mapNotNull null
                    val key = KeyGenerator.newKey()
                    val filename = FilenameFormatter.filename(
                        item = item,
                        defaultTitle = "Full Text",
                        ext = ext,
                        dateParser = dateParser
                    )
                    val attachment = Attachment(
                        type = Attachment.Kind.file(
                            filename = filename,
                            contentType = mimeType,
                            location = Attachment.FileLocation.local,
                            linkType = Attachment.FileLinkType.importedFile
                        ),
                        title = filename,
                        key = key,
                        libraryId = libraryId
                    )

                    attachment to url
                } ?: emptyList()

                return item to attachments
            } catch (error: Exception) {
                Timber.e(error, "IdentifierLookupController: can't parse data")
                return null
            }
        }

        fun process(
            identifier: String,
            response: ItemResponse,
            attachments: List<Pair<Attachment, String>>,
            libraryId: LibraryIdentifier,
            collectionKeys: Set<String>
        ) {
            fun storeDataAndDownloadAttachmentIfNecessary(
                identifier: String,
                response: ItemResponse,
                attachments: List<Pair<Attachment, String>>
            ) {
                val request = CreateTranslatedItemsDbRequest(
                    responses = listOf(response),
                    schemaController = schemaController,
                    dateParser = dateParser
                )
                dbWrapperMain.realmDbStorage.perform(request = request)
                changeLookup(
                    identifier = identifier,
                    state = LookupData.State.translated(
                        LookupData.State.TranslatedLookupData(
                            response = response,
                            attachments = attachments,
                            libraryId = libraryId,
                            collectionKeys = collectionKeys
                        )
                    )
                ) { didChange ->
                    if (!didChange) {
                        return@changeLookup
                    }
                    observable.emitAsync(
                        Update(
                            kind = Update.Kind.itemStored(
                                identifier = identifier,
                                response = response,
                                attachments = attachments
                            ), lookupData = lookupData.values.toList()
                        )
                    )
                    if (defaults.isShareExtensionIncludeAttachment() && !attachments.isEmpty()) {
                        val downloadData =
                            attachments.map { Triple(it.first, it.second, response.key) }
                        remoteFileDownloader.download(data = downloadData)
                        observable.emitAsync(
                            Update(
                                kind = Update.Kind.pendingAttachments(
                                    identifier = identifier,
                                    response = response,
                                    attachments = attachments
                                ), lookupData = lookupData.values.toList()
                            )
                        )
                    }

                    if (shouldSkipLookupsCleaning) {
                        return@changeLookup
                    }
                    cleanupLookupIfNeeded(force = false) { cleaned ->
                        if (!cleaned) {
                            observable.emitAsync(
                                Update(
                                    kind = Update.Kind.finishedAllLookups,
                                    lookupData = emptyList()
                                )
                            )
                        }
                    }
                }
            }

            try {
                storeDataAndDownloadAttachmentIfNecessary(
                    identifier = identifier,
                    response = response,
                    attachments = attachments
                )
            } catch (error: Exception) {
                Timber.e(error, "IdentifierLookupController: can't create item(s)")
                changeLookup(
                    identifier = identifier,
                    state = LookupData.State.failed
                ) { didChange ->
                    if (!didChange) {
                        return@changeLookup
                    }
                    observable.emitAsync(
                        Update(
                            kind = Update.Kind.itemCreationFailed(
                                identifier = identifier,
                                response = response,
                                attachments = attachments
                            ), lookupData = lookupData.values.toList()
                        )
                    )
                    if (shouldSkipLookupsCleaning) {
                        return@changeLookup
                    }
                    cleanupLookupIfNeeded(force = false) { cleaned ->
                        if (!cleaned) {
                            return@cleanupLookupIfNeeded
                        }
                        observable.emitAsync(
                            Update(
                                kind = Update.Kind.finishedAllLookups,
                                lookupData = emptyList()
                            )
                        )
                    }
                }
            }

        }


        fun process(result: Result<org.zotero.android.screens.addbyidentifier.data.LookupData>) {
            fun changeAndFinishAllLookups(identifier: String) {
                changeLookup(identifier, LookupData.State.failed) { didChange ->
                    if (!didChange) {
                        return@changeLookup
                    }
                    observable.emitAsync(
                        Update(
                            kind = Update.Kind.parseFailed(identifier = identifier),
                            lookupData = lookupData.values.toList()
                        )
                    )
                    if (shouldSkipLookupsCleaning) {
                        return@changeLookup
                    }
                    cleanupLookupIfNeeded(force = false) { cleaned ->
                        if (!cleaned) {
                            return@cleanupLookupIfNeeded
                        }
                        observable.emitAsync(
                            Update(
                                kind = Update.Kind.finishedAllLookups,
                                lookupData = emptyList()
                            )
                        )
                    }
                }
            }

            fun process(data: org.zotero.android.screens.addbyidentifier.data.LookupData) {
                when (data) {
                    is org.zotero.android.screens.addbyidentifier.data.LookupData.identifiers -> {
                        val identifiers = data.rawData
                        val enqueuedIdentifiers = identifiers.map {
                            val map = it.unmarshalMap<String, String>(this.gson)!!
                            identifier(map)
                        }
                        enqueueLookup(enqueuedIdentifiers) { validIdentifiers ->
                            if (validIdentifiers.isEmpty() && !shouldSkipLookupsCleaning) {
                                cleanupLookupIfNeeded(force = false) {
                                    observable.emitAsync(
                                        Update(
                                            kind = Update.Kind.identifiersDetected(
                                                identifiers = emptyList()
                                            ), lookupData = lookupData.values.toList()
                                        )
                                    )
                                }
                            }
                            observable.emitAsync(
                                Update(
                                    kind = Update.Kind.identifiersDetected(identifiers = validIdentifiers),
                                    lookupData = lookupData.values.toList()
                                )
                            )
                        }
                    }

                    is org.zotero.android.screens.addbyidentifier.data.LookupData.item -> {
                        val data = data.rawData
                        val lookupId = data["identifier"]?.unmarshalMap<String, String>(this.gson)
                        if (lookupId == null) {
                            Timber.w("IdentifierLookupController: lookup item data don't contain identifier")
                            return
                        }
                        val identifier = identifier(lookupId)
                        val currentLookupData = lookupData[identifier]
                        if (currentLookupData == null) {
                            return
                        }
                        val currentState = currentLookupData.state
                        if (!currentState.canTransition) {
                            Timber.w("IdentifierLookupController: $identifier lookup item can't transition from state: $currentState")
                            return
                        }
                        if (data.size() == 1) {
                            changeLookup(
                                identifier,
                                state = LookupData.State.inProgress
                            ) { didChange ->
                                if (!didChange) {
                                    return@changeLookup
                                }
                                observable.emitAsync(
                                    Update(
                                        kind = Update.Kind.lookupInProgress(
                                            identifier = identifier
                                        ), lookupData = lookupData.values.toList()
                                    )
                                )
                                // Since at least one identifier lookup is in progress, there is no need to cleanup if needed.
                            }
                            return
                        }
                        val error = data["error"]
                        if (error != null) {
                            Timber.e("IdentifierLookupController: $identifier lookup failed - $error")
                            changeLookup(
                                identifier = identifier,
                                state = LookupData.State.failed
                            ) { didChange ->
                                if (!didChange) {
                                    return@changeLookup
                                }
                                observable.emitAsync(
                                    Update(
                                        kind = Update.Kind.lookupFailed(
                                            identifier = identifier
                                        ), lookupData = lookupData.values.toList()
                                    )
                                )
                                if (shouldSkipLookupsCleaning) {
                                    return@changeLookup
                                }
                                cleanupLookupIfNeeded(force = false) { cleaned ->
                                    if (!cleaned) {
                                        return@cleanupLookupIfNeeded
                                    }
                                    observable.emitAsync(
                                        Update(
                                            kind = Update.Kind.finishedAllLookups,
                                            lookupData = emptyList()
                                        )
                                    )
                                }
                            }
                            return
                        }

                        val libraryId = lookupWebViewHandler.lookupSettings.libraryIdentifier
                        val collectionKeys = lookupWebViewHandler.lookupSettings.collectionKeys
                        val itemData = data["data"]?.asJsonArray
                        val item = itemData?.first()?.asJsonObject
                        if (item == null) {
                            changeAndFinishAllLookups(identifier)
                            return

                        }
                        val parseResult = parse(
                            item,
                            libraryId = libraryId,
                            collectionKeys = collectionKeys,
                            schemaController = schemaController,
                            dateParser = dateParser
                        )
                        if (parseResult == null) {
                            changeAndFinishAllLookups(identifier)
                            return
                        }
                        val (response, attachments) = parseResult
                        process(
                            identifier = identifier,
                            response = response,
                            attachments = attachments,
                            libraryId = libraryId,
                            collectionKeys = collectionKeys
                        )
                    }

                }

            }

            when (result) {
                is Result.Success -> {
                    process(result.value)
                }

                is Result.Failure -> {
                    Timber.e(result.exception, "IdentifierLookupController: lookup failed")
                    cleanupLookupIfNeeded(force = false) {
                        observable.emitAsync(
                            Update(
                                kind = Update.Kind.lookupError(result.exception),
                                lookupData = lookupData.values.toList()
                            )
                        )
                    }
                }

            }
        }

        lookupWebViewHandler.observable.flow()
            .onEach { result ->
                process(result)
            }
            .launchIn(mainCoroutineScope)
    }

    private fun enqueueLookup(identifiers: List<String>, completion: (List<String>) -> Unit) {
        val newUniqueIdentifiers = mutableListOf<String>()
        var index = 0
        for (identifier in identifiers) {
            if (lookupData[identifier] != null) {
                continue
            }
            newUniqueIdentifiers.add(identifier)
            lookupData[identifier] =
                LookupData(identifier = identifier, state = LookupData.State.enqueued)
            index += 1
        }
        completion(newUniqueIdentifiers)
    }

    private fun changeLookup(
        identifier: String,
        state: LookupData.State,
        completion: (Boolean) -> Unit
    ) {
        var didChange = false
        val currentLookupData = lookupData[identifier]
        if (currentLookupData == null) {
            completion(didChange)
            return
        }
        val currentState = currentLookupData.state
        val isTransitionValid = LookupData.State.isTransitionValid(from = currentState, to = state)
        if (!isTransitionValid) {
            Timber.w("IdentifierLookupController: $identifier lookup item won't transition from state: $currentState to state: $state")
            completion(didChange)
            return
        }

        lookupData[identifier] = LookupData(identifier = identifier, state = state)
        didChange = true
        when (state) {
            LookupData.State.failed -> {
                lookupFailedCount += 1
            }

            is LookupData.State.translated -> {
                lookupSavedCount += 1
            }

            else -> {
                //no-op
            }
        }
        completion(didChange)
    }


    private fun cleanupLookupIfNeeded(force: Boolean, completion: (Boolean) -> Unit) {
        fun cleanupLookup(force: Boolean, completion: (Boolean) -> Unit) {
            fun cleanup(completion: (Boolean) -> Unit) {
                lookupData = mutableMapOf()
                lookupSavedCount = 0
                lookupFailedCount = 0
                Timber.i("IdentifierLookupController: cleaned up lookup data")
                val keys = lookupWebViewHandlersByLookupSettings.keys
                for (key in keys) {
                    lookupWebViewHandlersByLookupSettings.remove(key)

                }
                completion(true)
            }
            if (force) {
                cleanup(completion = completion)
                return
            }
            if (lookupRemainingCount != 0 || remoteFileDownloader.batchData.second != 0) {
                completion(false)
                return
            }
            cleanup(completion = completion)

        }
        cleanupLookup(force = force, completion = completion)
    }

    fun trashItem(identifier: String, itemKey: String, libraryId: LibraryIdentifier) {
        lookupData.remove(identifier)
        val request = MarkItemsAsTrashedDbRequest(
            keys = listOf(itemKey),
            libraryId = libraryId,
            trashed = true
        )
        dbWrapperMain.realmDbStorage.perform(request)
    }

    suspend fun lookUp(
        libraryId: LibraryIdentifier,
        collectionKeys: Set<String>,
        identifier: String
    ) {
        val lookupSettings =
            LookupSettings(libraryIdentifier = libraryId, collectionKeys = collectionKeys)
        val lookupWebViewHandler = lookupWebViewHandlersByLookupSettings[lookupSettings]
        if (lookupWebViewHandler == null) {
            Timber.e("IdentifierLookupController: can't find lookup web view handler for settings - $lookupSettings")
            return
        }
        lookupWebViewHandler.lookUp(identifier = identifier)
    }

    fun cancelAllLookups(shouldTrashItems: Boolean = true) {
        Timber.i("IdentifierLookupController: cancel all lookups")
        val keys = lookupWebViewHandlersByLookupSettings.keys
        for (key in keys) {
            lookupWebViewHandlersByLookupSettings.remove(key)
        }
        remoteFileDownloader.stop()
        val lookupData = this.lookupData
        cleanupLookupIfNeeded(force = true) {
            if (this::observable.isInitialized) {
                this.observable.emitAsync(
                    Update(
                        kind = Update.Kind.finishedAllLookups,
                        lookupData = emptyList()
                    )
                )
            }
        }
        if (!shouldTrashItems || !this::observable.isInitialized) {
            return
        }
        val storedItemResponses = lookupData.values.mapNotNull {
            when (it.state) {
                is LookupData.State.translated -> {
                    val translatedLookupData = it.state.translatedLookupData
                    return@mapNotNull translatedLookupData.response to translatedLookupData.libraryId
                }

                else -> {
                    return@mapNotNull null
                }
            }
        }
        try {
            val requests = storedItemResponses.map {
                MarkItemsAsTrashedDbRequest(
                    keys = listOf(it.first.key),
                    libraryId = it.second,
                    trashed = true
                )
            }
            dbWrapperMain.realmDbStorage.perform(requests = requests)
        } catch (error: Exception) {
            Timber.e(error, "IdentifierLookupController: can't trash item(s)")
        }
    }

    data class Update(
        val kind: Kind,
        val lookupData: List<LookupData>,
    ) {
        sealed interface Kind {
            data class lookupError(val error: Exception) : Kind
            data class identifiersDetected(val identifiers: List<String>) : Kind
            data class lookupInProgress(val identifier: String) : Kind
            data class lookupFailed(val identifier: String) : Kind
            data class parseFailed(val identifier: String) : Kind
            data class itemCreationFailed(
                val identifier: String,
                val response: ItemResponse,
                val attachments: List<Pair<Attachment, String>>
            ) : Kind

            data class itemStored(
                val identifier: String,
                val response: ItemResponse,
                val attachments: List<Pair<Attachment, String>>
            ) : Kind

            data class pendingAttachments(
                val identifier: String,
                val response: ItemResponse,
                val attachments: List<Pair<Attachment, String>>
            ) : Kind

            object finishedAllLookups : Kind
        }
    }

    data class LookupData(
        val identifier: String,
        val state: State
    ) {
        sealed class State {
            object enqueued : State()
            object inProgress : State()
            object failed : State()
            data class translated(val translatedLookupData: TranslatedLookupData) : State()

            data class TranslatedLookupData(
                val response: ItemResponse,
                val attachments: List<Pair<Attachment, String>>,
                val libraryId: LibraryIdentifier,
                val collectionKeys: Set<String>,
            )

            val canTransition: Boolean
                get() {
                    when (this) {
                        enqueued, inProgress -> {
                            return true
                        }

                        is translated, failed -> {
                            return false
                        }
                    }
                }

            companion object {
                fun isTransitionValid(from: State, to: State): Boolean {
                    when {
                        from is enqueued && to is inProgress -> {
                            return true
                        }

                        from is enqueued && to is failed -> {
                            return true
                        }

                        from is inProgress && to is translated -> {
                            return true
                        }

                        from is inProgress && to is failed -> {
                            return true
                        }

                        else -> {
                            return false
                        }
                    }
                }
            }
        }
    }
}