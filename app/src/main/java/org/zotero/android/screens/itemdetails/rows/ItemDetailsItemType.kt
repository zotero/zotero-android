package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider

@Composable
internal fun ItemDetailsItemType(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onItemTypeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onItemTypeClicked
            )
    ) {
        Column {
            ItemDetailsFieldRow(
                detailTitle = stringResource(id = Strings.item_type),
                detailValue = viewState.data.localizedType,
                layoutType = layoutType,
            )
        }
        CustomDivider()
    }
}

@Composable
internal fun ItemDetailsItemType(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType
) {
    ItemDetailsFieldRow(
        detailTitle = stringResource(id = Strings.item_type),
        detailValue = viewState.data.localizedType,
        layoutType = layoutType,
    )
}