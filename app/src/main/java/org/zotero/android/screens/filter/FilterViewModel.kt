package org.zotero.android.screens.filter

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.allitems.data.ItemsState
import org.zotero.android.screens.filter.data.FilterResult
import javax.inject.Inject

@HiltViewModel
internal class FilterViewModel @Inject constructor(
) : BaseViewModel2<FilterViewState, FilterViewEffect>(FilterViewState()) {

    private val downloadsFilterEnabled: Boolean get() {
        val filters = ScreenArguments.filterArgs.filters
        return filters.any { it is ItemsState.Filter.downloadedFiles }
    }

    fun init() = initOnce {
        updateState {
            copy(isDownloadsChecked = downloadsFilterEnabled)
        }
    }

    fun onDone() {
        triggerEffect(FilterViewEffect.OnBack)
    }

    fun onDownloadsTapped() {
        updateState {
            copy(isDownloadsChecked = !viewState.isDownloadsChecked)
        }
        if (viewState.isDownloadsChecked) {
            EventBus.getDefault().post(FilterResult.enableFilter(ItemsState.Filter.downloadedFiles))
        } else {
            EventBus.getDefault().post(FilterResult.disableFilter(ItemsState.Filter.downloadedFiles))
        }
    }

}

internal data class FilterViewState(
    val isDownloadsChecked: Boolean = false,
) : ViewState

internal sealed class FilterViewEffect : ViewEffect {
    object OnBack : FilterViewEffect()
}