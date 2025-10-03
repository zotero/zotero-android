package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import org.zotero.android.screens.itemdetails.AddItemRow
import org.zotero.android.screens.itemdetails.ItemDetailHeaderSection
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

internal fun LazyListScope.itemDetailsEditListOfTags(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    item {
        NewSettingsDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailHeaderSection(Strings.item_detail_tags)
        }
    }
    items(
        viewState.tags
    ) { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {},
                    onLongClick = { viewModel.onTagLongClick(item) }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Drawables.tag),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier
                    .weight(1f),
                text = HtmlCompat.fromHtml(
                    item.name,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    item {
        AddItemRow(
            titleRes = Strings.item_detail_add_tag,
            onClick = viewModel::onAddTag
        )
    }
}
