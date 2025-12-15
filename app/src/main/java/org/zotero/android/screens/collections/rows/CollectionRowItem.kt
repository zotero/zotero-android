package org.zotero.android.screens.collections.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.prettyPrint
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable
import org.zotero.android.uicomponents.icon.IconWithPadding

@Composable
internal fun CollectionRowItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp,
    collection: Collection,
    selectedCollectionId: CollectionIdentifier,
    showCollectionItemCounts: Boolean,
    hasChildren: Boolean,
    isCollapsed: Boolean,
    onItemTapped: () -> Unit,
    onItemLongTapped: () -> Unit,
    onItemChevronTapped: () -> Unit,
) {
    var rowModifier: Modifier = Modifier.height(48.dp)
    if (layoutType.isTablet() && selectedCollectionId == collection.identifier) {
        rowModifier = rowModifier.background(color = MaterialTheme.colorScheme.secondaryContainer)
    }
    val arrowIconAreaSize = 48.dp
    val mainIconSize = 28.dp
    val paddingBetweenIconAndText = 12.dp
    val paddingBetweenArrowAndIcon = 4.dp
    val levelArrowAndExtraPadding = levelPadding + arrowIconAreaSize + paddingBetweenArrowAndIcon
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
            .debounceCombinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onItemTapped,
                onLongClick = onItemLongTapped
            )
    ) {
        if (!hasChildren) {
            Spacer(modifier = Modifier.width(levelArrowAndExtraPadding))
        } else {
            Spacer(modifier = Modifier.width(levelPadding))
            IconWithPadding(
                drawableRes = if (isCollapsed) {
                    Drawables.chevron_right_24px
                } else {
                    Drawables.expand_more_24px
                },
                onClick = { onItemChevronTapped() },
                areaSize = arrowIconAreaSize,
                shouldShowRipple = false,
                tintColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(paddingBetweenArrowAndIcon))
        }
        Icon(
            modifier = Modifier.size(mainIconSize),
            painter = painterResource(id = collection.iconName),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(paddingBetweenIconAndText))

        Text(
            modifier = Modifier.weight(1f),
            text = collection.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(16.dp))
        if ((!collection.isCollection || showCollectionItemCounts) && collection.itemCount != 0) {
            ItemCountText(count = collection.itemCount)
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun ItemCountText(
    count: Int,
) {
    Text(
        text = count.prettyPrint(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

}