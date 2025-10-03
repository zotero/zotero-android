package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.itemdetails.numberOfRowsInLazyColumnBeforeListOfCreatorsStarts
import org.zotero.android.screens.itemdetails.rows.ItemDetailsFieldRow
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.reorder.ReorderableState
import org.zotero.android.uicomponents.reorder.detectReorderAfterLongPress
import org.zotero.android.uicomponents.reorder.draggedItem

internal fun LazyListScope.itemDetailsEditListOfCreatorRows(
    viewState: ItemDetailsViewState,
    reorderState: ReorderableState,
    onDeleteCreator: (String) -> Unit,
    onCreatorClicked: (ItemDetailCreator) -> Unit,
) {
    for ((index, creatorId) in viewState.data.creatorIds.withIndex()) {
        val creator = viewState.data.creators[creatorId] ?: continue
        item {
            val title = creator.localizedType
            val value = creator.name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onCreatorClicked(creator) }
                    )
                    .draggedItem(reorderState.offsetByIndex(index + numberOfRowsInLazyColumnBeforeListOfCreatorsStarts)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(end = 32.dp)
                        .align(Alignment.Center)
                ) {
                    ItemDetailsFieldRow(
                        detailTitle = title,
                        detailValue = value,
                    )
                }
                IconButton(
                    modifier = Modifier.padding(start = 4.dp),
                    onClick = { onDeleteCreator(creatorId) }) {
                    Icon(
                        painter = painterResource(id = Drawables.do_not_disturb_on_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .detectReorderAfterLongPress(reorderState)
                ) {
                    Image(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp),
                        painter = painterResource(id = Drawables.drag_handle_24px),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}
