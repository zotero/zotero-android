package org.zotero.android.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.require
import org.zotero.android.dashboard.ui.ARG_ITEM_ID
import org.zotero.android.sync.ItemResultsUseCase
import javax.inject.Inject

@HiltViewModel
internal class ItemDetailsViewModel @Inject constructor(
    stateHandle: SavedStateHandle,
    private val itemResultsUseCase: ItemResultsUseCase,
) : BaseViewModel2<ItemDetailsViewState, ItemDetailsViewEffect>(ItemDetailsViewState()) {

    private val itemId: String by lazy {
        stateHandle.get<String>(ARG_ITEM_ID).require()
    }

    fun init() = initOnce {
        viewModelScope.launch {
            updateState {
                copy(itemResponse = itemResultsUseCase.getItemById(itemId))
            }
        }
    }

}

internal data class ItemDetailsViewState(
    val itemResponse: ItemResponse? = null
) : ViewState

internal sealed class ItemDetailsViewEffect : ViewEffect {
}
