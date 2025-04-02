package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.selector.MultiSelector
import org.zotero.android.uicomponents.selector.MultiSelectorOption

@Composable
internal fun SortPickerDisplayFields(
    sortByTitle: String,
    isAscending: Boolean,
    onSortFieldClicked: () -> Unit,
    onSortDirectionChanged: (Boolean) -> Unit,
) {
    SortPickerFieldTappableRow(
        detailTitle = stringResource(id = Strings.items_sort_by) + ": " + sortByTitle,
        onClick = onSortFieldClicked
    )
    val ascendingOption = MultiSelectorOption(1, stringResource(id = Strings.items_ascending))
    val descendingOption = MultiSelectorOption(2, stringResource(id = Strings.items_descending))
    MultiSelector(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .height(36.dp),
        options = listOf(
            ascendingOption,
            descendingOption
        ),
        selectedOptionId = if (isAscending) {
            ascendingOption.id
        } else {
            descendingOption.id
        },
        onOptionSelect = { onSortDirectionChanged(it == ascendingOption.id) },
        fontSize = 17.sp,
    )
}
