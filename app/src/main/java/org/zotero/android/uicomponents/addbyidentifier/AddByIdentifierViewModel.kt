package org.zotero.android.uicomponents.addbyidentifier

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AddByIdentifierViewModel @Inject constructor(
    private val fileStore: FileStore,
    private val identifierLookupController: IdentifierLookupController,
) : BaseViewModel2<AddByIdentifierViewState, AddByIdentifierViewEffect>(AddByIdentifierViewState()) {

    fun init() = initOnce {
        val collectionKeys =
            fileStore.getSelectedCollectionId().keyGet?.let { setOf(it) } ?: emptySet()
        val libraryId = fileStore.getSelectedLibrary()
        val restoreLookupState = ScreenArguments.addByIdentifierPickerArgs.restoreLookupState
        initState(
            restoreLookupState = restoreLookupState,
            hasDarkBackground = false,
            collectionKeys = collectionKeys,
            libraryId = libraryId
        )

        initialize(collectionKeys = collectionKeys, libraryId = libraryId)
    }

    private fun initialize(collectionKeys: Set<String>, libraryId: LibraryIdentifier) {
        identifierLookupController.initialize(
            libraryId = libraryId,
            collectionKeys = collectionKeys
        ) { lookupData ->
            if (lookupData == null) {
                Timber.e("LookupActionHandler: can't create observer")
                return@initialize
            }
            if (viewState.restoreLookupState && lookupData.isNotEmpty()) {
                Timber.i("AddByIdentifierVIewModel: restoring lookup state")
                updateState {
                    copy(lookupState = State.lookup(lookupData))
                }
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
                            updateState {
                                copy(lookupState = State.failed(update.kind.error))
                            }
                        }

                        is IdentifierLookupController.Update.Kind.identifiersDetected -> {
                            val identifiers = update.kind.identifiers
                            if (identifiers.isEmpty()) {
                                if (update.lookupData.isEmpty()) {
                                    updateState {
                                        copy(lookupState = State.failed(Error.noIdentifiersDetectedAndNoLookupData))
                                    }
                                } else {
                                    updateState {
                                        copy(lookupState = State.failed(Error.noIdentifiersDetectedWithLookupData))
                                    }
                                }
                                return@onEach
                            }
                            updateState {
                                copy(lookupState = State.lookup(update.lookupData))
                            }
                        }

                        is IdentifierLookupController.Update.Kind.lookupInProgress,
                        is IdentifierLookupController.Update.Kind.lookupFailed,
                        is IdentifierLookupController.Update.Kind.parseFailed,
                        is IdentifierLookupController.Update.Kind.itemCreationFailed,
                        is IdentifierLookupController.Update.Kind.itemStored,
                        is IdentifierLookupController.Update.Kind.pendingAttachments -> {
                            updateState {
                                copy(lookupState = State.lookup(update.lookupData))
                            }
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
                collectionKeys = collectionKeys,
                libraryId = libraryId,
                lookupState = AddByIdentifierViewModel.State.waitingInput,
                hasDarkBackground = hasDarkBackground,
            )
        }
    }

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
                updateState {
                    copy(lookupState = AddByIdentifierViewModel.State.loadingIdentifiers)
                }
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
    val collectionKeys: Set<String> = emptySet(),
    val libraryId: LibraryIdentifier = LibraryIdentifier.group(0),
    val restoreLookupState: Boolean = false,
    val hasDarkBackground: Boolean = false,
    var lookupState: AddByIdentifierViewModel.State = AddByIdentifierViewModel.State.waitingInput
) : ViewState {

}

internal sealed class AddByIdentifierViewEffect : ViewEffect {
    object NavigateBack : AddByIdentifierViewEffect()
}