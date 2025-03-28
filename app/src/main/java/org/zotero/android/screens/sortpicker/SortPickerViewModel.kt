package org.zotero.android.screens.sortpicker

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.sortpicker.data.SortDirectionResult
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerState
import javax.inject.Inject

@HiltViewModel
internal class SortPickerViewModel @Inject constructor(
    private val context: Context,
) : BaseViewModel2<SortPickerViewState, SortPickerViewEffect>(SortPickerViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItemsSortPicker) {
            viewModelScope.launch {
                val field = ItemsSortType.Field.entries.first { it.titleStr == singlePickerResult.id }
                updateState {
                    copy(sortByTitle = field.titleStr, isAscending = field.defaultOrderAscending )
                }
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val args = ScreenArguments.sortPickerArgs
        updateState {
            copy(sortByTitle = args.sortType.field.titleStr, isAscending = args.sortType.ascending)
        }
    }

    fun onDone() {
        triggerEffect(SortPickerViewEffect.OnBack)
    }

    fun onSortFieldClicked() {
        val pickerState = createSinglePickerState(
            selected = viewState.sortByTitle
        )
        ScreenArguments.singlePickerArgs =
            SinglePickerArgs(
                singlePickerState = pickerState,
                title = context.getString(Strings.items_sort_by),
                callPoint = SinglePickerResult.CallPoint.AllItemsSortPicker,
            )
        triggerEffect(SortPickerViewEffect.NavigateToSinglePickerScreen)
    }

    private fun createSinglePickerState(
        selected: String,
    ): SinglePickerState {

        val items = ItemsSortType.Field.entries.map {
            SinglePickerItem(id = it.titleStr, name = it.titleStr)
        }

        val state = SinglePickerState(objects = items, selectedRow = selected)
        return state
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    fun onSortDirectionChanged(isAscending: Boolean) {
        updateState {
            copy(isAscending = isAscending)
        }
        EventBus.getDefault().post(SortDirectionResult(isAscending))
    }
}

internal data class SortPickerViewState(
    val sortByTitle: String = "",
    val isAscending: Boolean = true,
) : ViewState

internal sealed class SortPickerViewEffect : ViewEffect {
    object OnBack: SortPickerViewEffect()
    object NavigateToSinglePickerScreen: SortPickerViewEffect()
}