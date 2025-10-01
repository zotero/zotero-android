package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

@Composable
internal fun ItemDetailsListOfCreatorRows(
    viewState: ItemDetailsViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onCreatorLongClick: (ItemDetailCreator) -> Unit,
) {
    for (creatorId in viewState.data.creatorIds) {
        val creator = viewState.data.creators[creatorId] ?: continue
        val title = creator.localizedType
        val value = creator.name
        Row(
            modifier = Modifier
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onLongClick = { onCreatorLongClick(creator) },
                    onClick = {}
                )
        ) {
            ItemDetailsFieldRow(
                detailTitle = title,
                detailValue = value,
                layoutType = layoutType,
            )
        }
    }

}
