package org.zotero.android.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.dashboard.SinglePickerResult
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.row.RowItemWithCheckbox
import org.zotero.android.uicomponents.topbar.CloseIconTopBar

@Composable
fun SinglePickerScreen(onCloseClicked: () -> Unit, scaffoldModifier: Modifier = Modifier) {
    val pickerArgs = ScreenArguments.singlePickerArgs
    val singlePickerState = pickerArgs.singlePickerState
    CustomScaffold(
        modifier = scaffoldModifier,
        topBar = {
            TopBar(
                title = pickerArgs.title,
                onCloseClicked = onCloseClicked
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(
                singlePickerState.objects
            ) {option ->
                RowItemWithCheckbox(
                    title = option.name,
                    checked = option.id == singlePickerState.selectedRow,
                    onCheckedChange = {
                        onCloseClicked()
                        EventBus.getDefault().post(SinglePickerResult(option.id))
                    }
                )
                CustomDivider(modifier = Modifier.padding(2.dp))
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String?,
    onCloseClicked: () -> Unit,
) {
    CloseIconTopBar(
        title = title,
        onClose = onCloseClicked,
    )
}


data class SinglePickerState(
    val objects: List<SinglePickerItem>,
    val selectedRow: String,
)

data class SinglePickerItem(
    val id: String,
    val name: String,
)