package org.zotero.android.screens.allitems.bottomsheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.zotero.android.uicomponents.modal.CustomModalBottomSheetM3

@Composable
internal fun AllItemsAddBottomSheet(
    onScanBarcode: () -> Unit,
    onAddFile: () -> Unit,
    onAddNote: () -> Unit,
    onAddManually: () -> Unit,
    onAddByIdentifier: () -> Unit,
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
        CustomModalBottomSheetM3(
            shouldCollapse = !showBottomSheet,
            sheetContent = {
                AllItemsAddBottomSheetContent(
                    onScanBarcode = {
                        onClose()
                        onScanBarcode()
                    },
                    onAddFile = {
                        onClose()
                        onAddFile()
                    }, onAddNote = {
                        onClose()
                        onAddNote()
                    },
                    onAddManually = {
                        onClose()
                        onAddManually()
                    },
                    onAddByIdentifier = {
                        onClose()
                        onAddByIdentifier()
                    }
                )
            },
            onCollapse = {
                shouldShow = false
                onClose()
            },
        )
    }
}
