package org.zotero.android.uicomponents.addbyidentifier

import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import javax.inject.Inject

@HiltViewModel
internal class AddByIdentifierViewModel @Inject constructor(
    private val gson: Gson,
) : BaseViewModel2<AddByIdentifierViewState, AddByIdentifierViewEffect>(AddByIdentifierViewState()) {

    fun init() = initOnce {
    }

    fun onLookup() {
        TODO("Not yet implemented")
    }

    fun onIsbnTextChange(newText: String) {
        updateState {
            copy(isbnText = newText)
        }
    }

}

internal data class AddByIdentifierViewState(
    val isbnText: String = "",
) : ViewState {

}

internal sealed class AddByIdentifierViewEffect : ViewEffect {
    object NavigateBack: AddByIdentifierViewEffect()
}