package org.zotero.android.uicomponents.addbyidentifier

import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject

@HiltViewModel
internal class AddByIdentifierViewModel @Inject constructor(
    private val gson: Gson,
    private val defaults: Defaults,
    private val fileStore: FileStore,
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
        TODO("Not yet implemented")
    }

    fun onIsbnTextChange(newText: String) {
        updateState {
            copy(isbnText = newText)
        }
    }

    sealed interface State {
        data class failed(val error: Exception) : State
        object waitingInput : State
        object loadingIdentifiers : State
        data class lookup(val data: List<IdentifierLookupController.LookupData>) : State
    }

}

internal data class AddByIdentifierViewState(
    val isbnText: String = "",
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