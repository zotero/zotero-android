package org.zotero.android.screens.allitems.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings

@Composable
internal fun AllItemsAddBottomSheetContent(
    onScanBarcode: () -> Unit,
    onAddFile: () -> Unit,
    onAddNote: () -> Unit,
    onAddManually: () -> Unit,
    onAddByIdentifier: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        AllItemsAddBottomSheetRow(
            title = stringResource(id = Strings.items_lookup),
            onClick = onAddByIdentifier
        )
        AllItemsAddBottomSheetRow(
            title = stringResource(id = Strings.items_barcode),
            onClick = onScanBarcode
        )
        AllItemsAddBottomSheetRow(
            title = stringResource(id = Strings.items_new),
            onClick = onAddManually
        )
        AllItemsAddBottomSheetRow(
            title = stringResource(id = Strings.items_new_note),
            onClick = onAddNote
        )
        AllItemsAddBottomSheetRow(
            title = stringResource(id = Strings.items_new_file),
            onClick = onAddFile
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
