package org.zotero.android.screens.allitems

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomModalBottomSheet
import org.zotero.android.uicomponents.row.RowItemWithArrow

@Composable
internal fun AllItemsAddBottomSheet(
    onAddFile:() -> Unit,
    onAddNote:() -> Unit,
    onAddManually:() -> Unit,
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
                AddBottomSheetContent(onAddFile = {
                    onClose()
                    onAddFile()
                }, onAddNote = {
                    onClose()
                    onAddNote()
                },
                    onAddManually = {
                        onClose()
                        onAddManually()
                    })
            },
            onCollapse = {
                shouldShow = false
                onClose()
            },
        )
    }
}

@Composable
private fun AddBottomSheetContent(
    onAddFile: () -> Unit,
    onAddNote: () -> Unit,
    onAddManually: () -> Unit,
) {
    Box {
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_manual),
                onClick = { onAddManually() }
            )
            CustomDivider(modifier = Modifier.padding(2.dp))
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_note),
                onClick = { onAddNote() }
            )
            CustomDivider(modifier = Modifier.padding(2.dp))
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_file),
                onClick = { onAddFile() }
            )
        }
    }
}