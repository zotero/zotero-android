package org.zotero.android.screens.sortpicker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings

@Composable
internal fun SortPickerSortDirectionSelector(
    viewState: SortPickerViewState,
    viewModel: SortPickerViewModel
) {
    val ascendingOptionLabel = stringResource(id = Strings.items_ascending)
    val descendingOptionLabel = stringResource(id = Strings.items_descending)

    val optionsList = listOf(ascendingOptionLabel, descendingOptionLabel)

    val selectedOptionIndex = if (viewState.isAscending) {
        optionsList.indexOf(ascendingOptionLabel)
    } else {
        optionsList.indexOf(descendingOptionLabel)
    }

    SingleChoiceSegmentedButtonRow {
        optionsList.forEachIndexed { index, opt ->
            val isSelected = index == selectedOptionIndex
            SegmentedButton(
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondary,
                    activeContentColor = MaterialTheme.colorScheme.onSecondary,
                    inactiveContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = optionsList.size,
                ),
                onClick = { viewModel.onSortDirectionChanged(isAscending = opt == ascendingOptionLabel) },
                selected = isSelected,
                label = {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            )
        }
    }
}