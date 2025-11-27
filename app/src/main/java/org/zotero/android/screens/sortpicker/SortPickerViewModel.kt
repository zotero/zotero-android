package org.zotero.android.screens.sortpicker

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.sortpicker.data.SortDirectionResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerState
import javax.inject.Inject

@HiltViewModel
internal class SortPickerViewModel @Inject constructor(
) : BaseViewModel2<SortPickerViewState, SortPickerViewEffect>(SortPickerViewState()) {

    fun init() = initOnce {
        val args = ScreenArguments.sortPickerArgs

        val pickerState = createSinglePickerState(
            selected = args.sortType.field.titleStr
        )

        updateState {
            copy(
                isAscending = args.sortType.ascending,
                sortByRows = pickerState.objects.toPersistentList(),
                selectedSortByRow = pickerState.selectedRow
            )
        }
    }

    fun onDone() {
        triggerEffect(SortPickerViewEffect.OnBack)
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

    fun onSortDirectionChanged(isAscending: Boolean) {
        updateState {
            copy(isAscending = isAscending)
        }
        EventBus.getDefault().post(SortDirectionResult(isAscending))
    }

    fun onSortByRowSelected(option: SinglePickerItem) {
        val field =
            ItemsSortType.Field.entries.first { it.titleStr == option.id }
        updateState {
            copy(isAscending = field.defaultOrderAscending, selectedSortByRow = option.id)
        }
        EventBus
            .getDefault()
            .post(SinglePickerResult(option.id, SinglePickerResult.CallPoint.AllItemsSortPicker))
    }
}

internal data class SortPickerViewState(
    val isAscending: Boolean = true,

    val sortByRows: PersistentList<SinglePickerItem> = persistentListOf(),
    val selectedSortByRow: String = "",
) : ViewState

internal sealed class SortPickerViewEffect : ViewEffect {
    object OnBack : SortPickerViewEffect()
}