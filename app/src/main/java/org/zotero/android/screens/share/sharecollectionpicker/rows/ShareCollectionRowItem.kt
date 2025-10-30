package org.zotero.android.screens.share.sharecollectionpicker.rows

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable
import org.zotero.android.uicomponents.icon.IconWithPadding

@Composable
internal fun ShareCollectionRowItem(
    levelPadding: Dp,
    @DrawableRes iconRes: Int,
    title: String,
    isCollapsed: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    forceUpdateKey: String,
    onItemChevronTapped: () -> Unit,
    onRowTapped: () -> Unit,
) {
    var rowModifier: Modifier = Modifier.height(48.dp)
    if (isSelected) {
        rowModifier = rowModifier.background(color = MaterialTheme.colorScheme.secondaryContainer)
    }
    val arrowIconAreaSize = 48.dp
    val mainIconSize = 28.dp
    val paddingBetweenIconAndText = 12.dp
    val levelPaddingWithArrowIconAreaSize = levelPadding + arrowIconAreaSize
    key(forceUpdateKey) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onRowTapped,
                )
        ) {
            if (!hasChildren) {
                Spacer(modifier = Modifier.width(levelPaddingWithArrowIconAreaSize))
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
            }
            Icon(
                modifier = Modifier.size(mainIconSize),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(paddingBetweenIconAndText))

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

    }

}
