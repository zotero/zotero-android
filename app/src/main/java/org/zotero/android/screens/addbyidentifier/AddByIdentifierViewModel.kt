package org.zotero.android.screens.addbyidentifier

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.emptyImmutableSet
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.navigation.phone.ARG_ADD_BY_IDENTIFIER
import org.zotero.android.architecture.require
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloader
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloaderEventStream
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.files.FileStore
import org.zotero.android.screens.addbyidentifier.data.AddByIdentifierPickerArgs
import org.zotero.android.screens.addbyidentifier.data.ISBNParser
import org.zotero.android.screens.addbyidentifier.data.IdentifierLookupMode
import org.zotero.android.screens.addbyidentifier.data.LookupRow
import org.zotero.android.screens.addbyidentifier.data.LookupRowItem
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AddByIdentifierViewModel @Inject constructor(
    private val fileStore: FileStore,
    private val identifierLookupController: IdentifierLookupController,
    private val attachmentDownloaderEventStream: RemoteAttachmentDownloaderEventStream,
    private val schemaController: SchemaController,
    private val remoteFileDownloader: RemoteAttachmentDownloader,
    stateHandle: SavedStateHandle,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
) : BaseViewModel2<AddByIdentifierViewState, AddByIdentifierViewEffect>(AddByIdentifierViewState()) {

    private val screenArgs: AddByIdentifierPickerArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_ADD_BY_IDENTIFIER).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private val scannerPatternRegex =
        "10.\\d{4,9}\\/[-._;()\\/:a-zA-Z0-9]+"

    fun init() = initOnce {
        viewModelScope.launch {
            setupAttachmentObserving()
            val collectionKeys =
                fileStore.getSelectedCollectionIdAsync().keyGet?.let { setOf(it) } ?: emptySet()
            val libraryId = fileStore.getSelectedLibraryAsync()
            val restoreLookupState = screenArgs.restoreLookupState
            initState(
                restoreLookupState = restoreLookupState,
                hasDarkBackground = false,
                collectionKeys = collectionKeys,
                libraryId = libraryId
            )

            initialize(collectionKeys = collectionKeys, libraryId = libraryId)
        }
    }

    private fun initialize(collectionKeys: Set<String>, libraryId: LibraryIdentifier) {
        identifierLookupController.initialize(
            lookupMode = IdentifierLookupMode.normal,
            libraryId = libraryId,
            collectionKeys = collectionKeys
        ) { lookupData ->
            if (lookupData == null) {
                Timber.e("LookupActionHandler: can't create observer")
                return@initialize
            }
            if (viewState.restoreLookupState && lookupData.isNotEmpty()) {
                Timber.i("AddByIdentifierVIewModel: restoring lookup state")
                updateLookupState(State.lookup(lookupData))
            }
            identifierLookupController.observable
                .flow()
                .onEach { update ->
                    when (viewState.lookupState) {
                        is State.failed, State.waitingInput -> {
                            return@onEach
                        }

                        else -> {
                            //no-op
                        }
                    }
                    when (update.kind) {
                        is IdentifierLookupController.Update.Kind.lookupError -> {
                            updateLookupState(State.failed(update.kind.error))
                        }

                        is IdentifierLookupController.Update.Kind.identifiersDetected -> {
                            val identifiers = update.kind.identifiers
                            if (identifiers.isEmpty()) {
                                if (update.lookupData.isEmpty()) {
                                    updateLookupState(State.failed(Error.noIdentifiersDetectedAndNoLookupData))
                                } else {
                                    updateLookupState(State.failed(Error.noIdentifiersDetectedWithLookupData))
                                }
                                return@onEach
                            }
                            updateLookupState(State.lookup(update.lookupData))
                        }

                        is IdentifierLookupController.Update.Kind.lookupInProgress,
                        is IdentifierLookupController.Update.Kind.lookupFailed,
                        is IdentifierLookupController.Update.Kind.parseFailed,
                        is IdentifierLookupController.Update.Kind.itemCreationFailed,
                        is IdentifierLookupController.Update.Kind.itemStored,
                        is IdentifierLookupController.Update.Kind.pendingAttachments -> {
                            updateLookupState(State.lookup(update.lookupData))
                        }

                        IdentifierLookupController.Update.Kind.finishedAllLookups -> {//no-op}

                        }
                    }


                }.launchIn(viewModelScope)
        }
    }

    private fun initState(
        restoreLookupState: Boolean,
        hasDarkBackground: Boolean,
        collectionKeys: Set<String>,
        libraryId: LibraryIdentifier
    ) {
        updateState {
            copy(
                restoreLookupState = restoreLookupState,
                collectionKeys = collectionKeys.toImmutableSet(),
                libraryId = libraryId,
                hasDarkBackground = hasDarkBackground,
            )
        }
        updateLookupState(State.waitingInput)
    }

    fun process(scannedText: String) {
        val identifiers = Regex(scannerPatternRegex).findAll(scannedText).map { it.value }.toMutableList()
        val isbns = ISBNParser.isbns(scannedText)
        if (isbns.isNotEmpty()) {
            identifiers.addAll(isbns)
        }

        if (identifiers.isEmpty()) {
            return
        }

        val scannedText = identifiers.joinToString(", ")

        var newText = viewState.identifierText
        if (newText.isEmpty()) {
            newText = scannedText
        } else {
            newText += ", $scannedText"
        }
        updateState {
            copy(identifierText = newText)
        }
    }

//    fun onScanText() {
//        val scanner = GmsBarcodeScanning.getClient(context)
//        scanner.startScan()
//            .addOnSuccessListener { barcode ->
//                val scannedString = barcode.rawValue ?: ""
//                process(scannedString)
//            }
//            .addOnCanceledListener {
//                // Task canceled
//            }
//            .addOnFailureListener { e ->
//               Timber.e(e, "Barcode scanning failed")
//            }
//    }

    fun onLookup() {
        val identifier = viewState.identifierText.trim()
        if (identifier.isBlank()) {
            return
        }
        val newIdentifier = identifier.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }
            .joinToString(separator = ",")

        if (newIdentifier.isEmpty()) {
            return
        }
        when (viewState.lookupState) {
            State.waitingInput, is State.failed -> {
                updateLookupState(State.loadingIdentifiers)
            }

            State.loadingIdentifiers, is State.lookup -> {
                //no-op
            }
        }

        val collectionKeys = viewState.collectionKeys
        val libraryId = viewState.libraryId
        viewModelScope.launch {
            identifierLookupController.lookUp(
                libraryId = libraryId,
                collectionKeys = collectionKeys,
                identifier = newIdentifier
            )
        }
    }


    fun onIdentifierTextChange(newText: String) {
        updateState {
            copy(identifierText = newText)
        }
    }

    fun cancelAllLookups() {
        identifierLookupController.cancelAllLookups()
        updateLookupState(State.waitingInput)
    }

    override fun onCleared() {
        identifierLookupController.cancelAllLookups()
        super.onCleared()
    }

    private fun setupAttachmentObserving() {
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                process(update = update)
                closeAfterUpdateIfNeeded()

            }.launchIn(viewModelScope)
    }

    private fun updateLookupState(lookupState: State) {
        updateState {
            copy(lookupState = lookupState)
        }
        val rowsList = mutableListOf<LookupRow>()
        if (lookupState is State.lookup) {
            val data = lookupState.data
            for (lookup in data) {
                when (lookup.state) {
                    IdentifierLookupController.LookupData.State.enqueued -> {
                        rowsList.add(
                            LookupRow.identifier(
                                identifier = lookup.identifier,
                                state = LookupRow.IdentifierState.enqueued
                            )
                        )
                    }

                    IdentifierLookupController.LookupData.State.failed -> {
                        rowsList.add(
                            LookupRow.identifier(
                                identifier = lookup.identifier,
                                state = LookupRow.IdentifierState.failed
                            )
                        )
                    }

                    IdentifierLookupController.LookupData.State.inProgress -> {
                        rowsList.add(
                            LookupRow.identifier(
                                identifier = lookup.identifier,
                                state = LookupRow.IdentifierState.inProgress
                            )
                        )
                    }

                    is IdentifierLookupController.LookupData.State.translatedAndParsedAttachments -> {
                        val translationData = lookup.state.translatedLookupData
                        val title: String
                        val _title = translationData.response.fields[KeyBaseKeyPair(
                            key = FieldKeys.Item.title,
                            baseKey = null
                        )]
                        if (_title != null) {
                            title = _title
                        } else {
                            val _title = translationData.response.fields.entries.firstOrNull {
                                this.schemaController.baseKey(
                                    type = translationData.response.rawType,
                                    field = it.key.key
                                ) == FieldKeys.Item.title
                            }?.value
                            title = _title ?: ""
                        }
                        val itemData =
                            LookupRowItem(
                                identifier = lookup.identifier,
                                key = translationData.response.key,
                                type = translationData.response.rawType,
                                title = title
                            )

                        rowsList.add(LookupRow.item(itemData))

                        val attachments = translationData.attachments.map { attachment ->
                            val (progress, error) = this.remoteFileDownloader.data(
                                attachment.first.key,
                                parentKey = translationData.response.key,
                                libraryId = attachment.first.libraryId
                            )
                            val updateKind: RemoteAttachmentDownloader.Update.Kind = if (error != null) {
                                RemoteAttachmentDownloader.Update.Kind.failed
                            } else if (progress != null) {
                                RemoteAttachmentDownloader.Update.Kind.progress(progress)
                            } else {
                                RemoteAttachmentDownloader.Update.Kind.ready(attachment.first)
                            }
                            return@map LookupRow.attachment(
                                attachment = attachment.first,
                                updateKind = updateKind
                            )
                        }
                        rowsList.addAll(attachments)
                    }

                    is IdentifierLookupController.LookupData.State.translatedAndCreatedItem -> {
                        //no-op
                    }
                    is IdentifierLookupController.LookupData.State.translatedOnly -> {
                        //no-op
                    }
                }
            }
        }
        updateState {
            copy(lookupRows = rowsList.toPersistentList())
        }
        closeAfterUpdateIfNeeded()

    }

    private fun closeAfterUpdateIfNeeded() {
        val itemIdentifiers = viewState.lookupRows
        if (itemIdentifiers.isEmpty()) {
            return
        }
        val hasActiveDownload = itemIdentifiers.any { row ->
            when (row) {
                is LookupRow.attachment -> {
                    when (row.updateKind) {
                        is RemoteAttachmentDownloader.Update.Kind.progress, RemoteAttachmentDownloader.Update.Kind.failed -> {
                            return@any true
                        }

                        else -> {
                            return@any false
                        }
                    }
                }

                is LookupRow.identifier -> {
                    return@any true
                }

                is LookupRow.item -> {
                    return@any false
                }
            }
        }
        if (!hasActiveDownload) {
            triggerEffect(AddByIdentifierViewEffect.NavigateBack)
        }
    }

    private fun process(update: RemoteAttachmentDownloader.Update) {
        if (update.download.libraryId != viewState.libraryId) {
            return
        }
        if (viewState.lookupRows.isEmpty()) {
            return
        }

        val rows = viewState.lookupRows.toMutableList()
        val index = rows.indexOfFirst {
            it.isAttachment(
                update.download.key,
                libraryId = update.download.libraryId
            )
        }
        if (index == -1) {
            return
        }

        val row = rows[index]
        when (row) {
            is LookupRow.attachment -> {
                rows[index] = LookupRow.attachment(
                    attachment = row.attachment,
                    updateKind = update.kind
                )
            }

            is LookupRow.item, is LookupRow.identifier -> {
                //no-op
            }
        }
        updateState {
            copy(lookupRows = rows.toPersistentList())
        }
    }


    sealed interface State {
        data class failed(val error: Exception) : State
        object waitingInput : State
        object loadingIdentifiers : State
        data class lookup(val data: List<IdentifierLookupController.LookupData>) : State
    }

    sealed class Error : Exception() {
        object noIdentifiersDetectedAndNoLookupData : Error()
        object noIdentifiersDetectedWithLookupData : Error()

    }

}

internal data class AddByIdentifierViewState(
    val identifierText: String = "",
    val collectionKeys: ImmutableSet<String> = emptyImmutableSet(),
    val libraryId: LibraryIdentifier = LibraryIdentifier.group(0),
    val restoreLookupState: Boolean = false,
    val hasDarkBackground: Boolean = false,
    val lookupState: AddByIdentifierViewModel.State = AddByIdentifierViewModel.State.waitingInput,
    val lookupRows: ImmutableList<LookupRow> = persistentListOf(),
) : ViewState

internal sealed class AddByIdentifierViewEffect : ViewEffect {
    object NavigateBack : AddByIdentifierViewEffect()
}