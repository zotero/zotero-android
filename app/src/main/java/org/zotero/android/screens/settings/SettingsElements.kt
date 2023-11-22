package org.zotero.android.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
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
    layoutType: CustomLayoutSize.LayoutType,
    isLastItem: Boolean,
    textColor: Color = CustomTheme.colors.primaryContent,
    onItemTapped: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { onItemTapped() },
            )
            .background(CustomTheme.colors.surface)

    ) {
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = layoutType.calculateTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!isLastItem) {
                SidebarDivider()
            }
        }
    }
}

@Composable
internal fun SettingsSectionTitle(
    layoutType: CustomLayoutSize.LayoutType,
    @StringRes titleId: Int
) {
    androidx.compose.material3.Text(
        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
        text = stringResource(id = titleId),
        fontSize = layoutType.calculateSettingsSectionTextSize(),
        color = CustomTheme.colors.secondaryContent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}