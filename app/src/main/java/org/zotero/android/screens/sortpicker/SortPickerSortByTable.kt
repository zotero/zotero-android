package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import org.zotero.android.uicomponents.singlepicker.SinglePickerRadioButtonRow

internal fun LazyListScope.sortPickerSortByTable(
    viewState: SortPickerViewState,
    viewModel: SortPickerViewModel
) {
    items(
        viewState.sortByRows
    ) { option ->
        SinglePickerRadioButtonRow(
            text = option.name,
            isSelected = option.id == viewState.selectedSortByRow,
            onOptionSelected = {
                viewModel.onSortByRowSelected(option)
            }
        )
    }


}
