package org.zotero.android.screens.allitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun AllItemsBottomPanelAppbarContent(
    overflowTextResId: Int,
    onClick: () -> Unit,
    iconRes: Int,
    iconTint: Color?
) {
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above, 4.dp),
        tooltip = {
            PlainTooltip() { Text(stringResource(overflowTextResId)) }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = decideIconTintToUse(iconTint),
            )
        }

    }
}

@Composable
internal fun AllItemsBottomPanelMenuContent(
    onClick: () -> Unit,
    iconRes: Int,
    iconTint: Color?,
    overflowTextResId: Int,
    textColor: Color?
) {
    Row(
        modifier = Modifier
            .widthIn(min = 160.dp)
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = decideIconTintToUse(iconTint),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(overflowTextResId),
            color = decideTextColorToUse(textColor),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun decideIconTintToUse(iconTint: Color?): Color {
    return iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun decideTextColorToUse(textColor: Color?): Color {
    return textColor ?: MaterialTheme.colorScheme.onSurface
}