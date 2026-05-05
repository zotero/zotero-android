package org.zotero.android.screens.htmlepub.reader.web.actionmenu

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun HtmlEpubActionMenuAppbarContentItem(
    overflowTextResId: Int,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = stringResource(overflowTextResId),
            color = CustomTheme.colors.primaryContent,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
internal fun HtmlEpubActionMenuContentItem(
    onClick: () -> Unit,
    overflowTextResId: Int,
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
        Text(
            text = stringResource(overflowTextResId),
            color = CustomTheme.colors.primaryContent,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
internal fun HtmlEpubActionCollapseOverflowItem(
    onClick: () -> Unit,
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
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(Drawables.arrow_back_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}