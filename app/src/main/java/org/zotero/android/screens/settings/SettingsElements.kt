package org.zotero.android.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsSection(
    content: @Composable ColumnScope.() -> Unit
) {
    val roundCornerShape = RoundedCornerShape(size = 10.dp)
    Column(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = roundCornerShape
            )
            .clip(roundCornerShape),
    ) {
        content()
    }

}

@Composable
internal fun SettingsItem(
    title: String,
    textColor: Color = CustomTheme.colors.primaryContent,
    onItemTapped: () -> Unit,
    onItemLongTapped: (() -> Unit)? = null,
    addNewScreenNavigationIndicator: Boolean = false,
    chevronNavigationColor: Color = CustomTheme.colors.chevronNavigationColor,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onItemTapped() },
                onLongClick = onItemLongTapped
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.padding(vertical = 10.dp).padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = textColor,
        )
        if (addNewScreenNavigationIndicator) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = Drawables.chevron_right_24px),
                contentDescription = null,
                tint = chevronNavigationColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
internal fun SettingsDivider() {
    Box(modifier = Modifier.background(CustomTheme.colors.surface)) {
        SidebarDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
internal fun SettingsSectionTitle(
    @StringRes titleId: Int
) {
    androidx.compose.material3.Text(
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        text = stringResource(id = titleId).uppercase(),
        fontSize = 14.sp,
        color = CustomTheme.colors.secondaryContent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}