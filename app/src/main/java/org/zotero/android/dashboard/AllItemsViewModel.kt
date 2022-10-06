package org.zotero.android.dashboard

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.sync.ItemResultsUseCase
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import javax.inject.Inject

@HiltViewModel
internal class AllItemsViewModel @Inject constructor(
    private val itemResultsUseCase: ItemResultsUseCase,
) : BaseViewModel2<AllItemsViewState, AllItemsViewEffect>(AllItemsViewState()) {

    fun init(lifecycleOwner: LifecycleOwner) = initOnce {
        viewModelScope.launch {
            itemResultsUseCase.resultLiveData.observe(lifecycleOwner) {
                when (it) {
                    is CustomResult.GeneralSuccess -> {
                        if (it.value.isNotEmpty()) {
                            updateState {
                                copy(lce = LCE2.Content, items = it.value)
                            }
                        }
                    }
                    else -> {
                        updateState {
                            copy(lce = LCE2.LoadError {})
                        }
                    }
                }

            }
        }
    }

}

internal data class AllItemsViewState(
    val lce: LCE2 = LCE2.Loading,
    val snackbarMessage: SnackbarMessage? = null,
    val items: List<ItemResponse> = emptyList()
) : ViewState

internal sealed class AllItemsViewEffect : ViewEffect {
}
