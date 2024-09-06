package org.zotero.android.screens.scanbarcode

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.androidx.content.longToast
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloader
import org.zotero.android.attachmentdownloader.RemoteAttachmentDownloaderEventStream
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.files.FileStore
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewEffect.NavigateBack
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewModel.State
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.addbyidentifier.IdentifierLookupController
import org.zotero.android.uicomponents.addbyidentifier.TranslatorLoadedEventStream
import org.zotero.android.uicomponents.addbyidentifier.data.LookupRow
import org.zotero.android.uicomponents.addbyidentifier.data.LookupRowItem
import timber.log.Timber
import java.util.LinkedList
import javax.inject.Inject

@HiltViewModel
internal class ScanBarcodeViewModel @Inject constructor(
    private val fileStore: FileStore,
    private val identifierLookupController: IdentifierLookupController,
    private val attachmentDownloaderEventStream: RemoteAttachmentDownloaderEventStream,
    private val translatorLoadedEventStream: TranslatorLoadedEventStream,
    private val schemaController: SchemaController,
    private val remoteFileDownloader: RemoteAttachmentDownloader,
    private val context: Context,
) : BaseViewModel2<ScanBarcodeViewState, ScanBarcodeViewEffect>(ScanBarcodeViewState()) {

    private val queueOfScannedBarcodes = LinkedList<String>()

    fun init() = initOnce {
        setupTranslatorLoadedObserving()
        setupAttachmentObserving()
        val collectionKeys =
            fileStore.getSelectedCollectionId().keyGet?.let { setOf(it) } ?: emptySet()
        val libraryId = fileStore.getSelectedLibrary()
        initState(
            hasDarkBackground = false,
            collectionKeys = collectionKeys,
            libraryId = libraryId
        )

        initialize(collectionKeys = collectionKeys, libraryId = libraryId)

        launchBarcodeScanner()
    }

    fun launchBarcodeScanner() {
        val scanner = GmsBarcodeScanning.getClient(context)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val scannedString = barcode.rawValue ?: ""
                if (translatorLoadedEventStream.currentValue() == true) {
                    onLookup(scannedString)
                } else {
                   queueOfScannedBarcodes.add(scannedString)
                }
            }
            .addOnCanceledListener {
                triggerEffect(NavigateBack)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Barcode scanning failed")
                context.longToast("Barcode scanning is not supported on your device")
                triggerEffect(NavigateBack)
            }
    }

    private fun initialize(collectionKeys: Set<String>, libraryId: LibraryIdentifier) {
        identifierLookupController.initialize(
            libraryId = libraryId,
            shouldSkipLookupsCleaning = true,
            collectionKeys = collectionKeys
        ) { lookupData ->
            if (lookupData == null) {
                Timber.e("LookupActionHandler: can't create observer")
                return@initialize
            }
            if (lookupData.isNotEmpty()) {
                Timber.i("ScanBarcodeViewModel: restoring lookup state")
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
                                    context.longToast(Strings.errors_lookup)
                                } else {
                                    context.longToast(Strings.scar_barcode_error_lookup_no_new_identifiers_found)
                                }
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
        hasDarkBackground: Boolean,
        collectionKeys: Set<String>,
        libraryId: LibraryIdentifier
    ) {
        updateState {
            copy(
                collectionKeys = collectionKeys,
                libraryId = libraryId,
                hasDarkBackground = hasDarkBackground,
            )
        }
        updateLookupState(State.waitingInput)
    }

    private fun onLookup(identifier: String) {
        if (identifier.isBlank()) {
            context.longToast("Failed to scan barcode")
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

    private fun setupAttachmentObserving() {
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                process(update = update)
            }.launchIn(viewModelScope)
    }

    private fun setupTranslatorLoadedObserving() {
        translatorLoadedEventStream.emit(false) //reset the translator loaded indicator before initializing the translator
        translatorLoadedEventStream.flow()
            .filter { it == true }
            .onEach { _ ->
                queueOfScannedBarcodes.forEach {
                    onLookup(it)
                }
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

                    is IdentifierLookupController.LookupData.State.translated -> {
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
                            val updateKind: RemoteAttachmentDownloader.Update.Kind
                            if (error != null) {
                                updateKind = RemoteAttachmentDownloader.Update.Kind.failed
                            } else if (progress != null) {
                                updateKind =
                                    RemoteAttachmentDownloader.Update.Kind.progress(progress)
                            } else {
                                updateKind =
                                    RemoteAttachmentDownloader.Update.Kind.ready(attachment.first)
                            }
                            return@map LookupRow.attachment(
                                attachment = attachment.first,
                                updateKind = updateKind
                            )
                        }
                        rowsList.addAll(attachments)
                    }
                }
            }
        }
        updateState {
            copy(lookupRows = rowsList)
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
            copy(lookupRows = rows)
        }
    }

    override fun onCleared() {
        identifierLookupController.cancelAllLookups(shouldTrashItems = false)
        super.onCleared()
    }

    fun onItemDelete(lookupRow: LookupRow.item) {
        val state = viewState.lookupState as State.lookup
        val item = state.data.find {
            val translatedItem = it.state as IdentifierLookupController.LookupData.State.translated
            translatedItem.translatedLookupData.response.key == lookupRow.item.key
        }!!
        val translatedItem = item.state as IdentifierLookupController.LookupData.State.translated

        identifierLookupController.trashItem(
            identifier = lookupRow.item.identifier,
            itemKey = translatedItem.translatedLookupData.response.key,
            libraryId = translatedItem.translatedLookupData.libraryId
        )

        val itemIndex = viewState.lookupRows.indexOf(lookupRow)
        val lookupRowsMutable = viewState.lookupRows.toMutableList()
        lookupRowsMutable.remove(lookupRow)
        if (lookupRowsMutable.isNotEmpty() && itemIndex < lookupRowsMutable.size && lookupRowsMutable[itemIndex] is LookupRow.attachment) {
            lookupRowsMutable.removeAt(itemIndex)
        }

        val mutableStateData = state.data.toMutableList()
        mutableStateData.remove(item)
        val updatedLookupState = State.lookup(mutableStateData)

        updateState {
            copy(
                lookupRows = lookupRowsMutable,
                lookupState = updatedLookupState
            )
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

internal data class ScanBarcodeViewState(
    val collectionKeys: Set<String> = emptySet(),
    val libraryId: LibraryIdentifier = LibraryIdentifier.group(0),
    val hasDarkBackground: Boolean = false,
    val lookupState: State = State.waitingInput,
    val lookupRows: List<LookupRow> = emptyList(),
) : ViewState

internal sealed class ScanBarcodeViewEffect : ViewEffect {
    object NavigateBack : ScanBarcodeViewEffect()
}