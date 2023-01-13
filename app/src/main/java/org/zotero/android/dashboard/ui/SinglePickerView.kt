package org.zotero.android.dashboard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomModalBottomSheet
import org.zotero.android.uicomponents.row.RowItemWithCheckbox


@Composable
internal fun SinglePickerViewBottomSheet(
    singlePickerState: SinglePickerState,
    onOptionSelected: (String) -> Unit,
    onClose: () -> Unit,
    showBottomSheet: Boolean,
) {

    var shouldShow by remember { mutableStateOf(false) }
    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            shouldShow = true
        }
    }

    if (shouldShow) {
        CustomModalBottomSheet(
            shouldCollapse = !showBottomSheet,
            sheetContent = {
                SinglePickerViewSheetContent(onOptionSelected = {
                    onClose()
                    onOptionSelected(it)
                },
                    singlePickerState = singlePickerState)
            },
            onCollapse = {
                shouldShow = false
                onClose()
            },
        )
    }

}

@Composable
private fun SinglePickerViewSheetContent(
    onOptionSelected:(String) -> Unit,
    singlePickerState: SinglePickerState,
) {
    Box {
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            for (option in singlePickerState.objects) {
                RowItemWithCheckbox(
                    title = option.name,
                    checked = option.id == singlePickerState.selectedRow,
                    onCheckedChange = { onOptionSelected(option.id) }
                )
                CustomDivider(modifier = Modifier.padding(2.dp))
            }

        }
    }
}

data class SinglePickerState(
    val objects: List<SinglePickerItem>,
    val selectedRow: String,
)

data class SinglePickerItem(
    val id: String,
    val name: String,
)