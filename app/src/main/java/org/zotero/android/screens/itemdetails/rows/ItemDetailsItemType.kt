package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ItemDetailsItemType(
    viewState: ItemDetailsViewState,
    onItemTypeClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
        , verticalAlignment = Alignment.CenterVertically
    ) {
        ItemDetailsFieldRow(
            detailTitle = stringResource(id = Strings.item_type),
            detailValue = viewState.data.localizedType,
            onRowTapped = onItemTypeClicked
        )
    }

}
