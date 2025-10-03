package org.zotero.android.screens.itemdetails.rows

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
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
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable

internal fun LazyListScope.itemDetailsListOfNotes(
    sectionTitle: Int,
    @DrawableRes itemIcon: Int,
    itemTitles: List<String>,
    onItemClicked: (Int) -> Unit,
    onItemLongClicked: (Int) -> Unit,
    @StringRes addTitleRes: Int,
    onAddItemClick: (() -> Unit)? = null,
) {
    item {
        NewSettingsDivider()
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            ItemDetailHeaderSection(sectionTitle)
        }
    }
    itemsIndexed(
        itemTitles
    ) { index, item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = { onItemClicked(index) },
                    onLongClick = { onItemLongClicked(index) }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = itemIcon),
                modifier = Modifier.size(28.dp),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                modifier = Modifier
                    .weight(1f),
                text = HtmlCompat.fromHtml(
                    item,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (onAddItemClick != null) {
        item {
            AddItemRow(
                titleRes = addTitleRes,
                onClick = onAddItemClick
            )
        }
    }

}
