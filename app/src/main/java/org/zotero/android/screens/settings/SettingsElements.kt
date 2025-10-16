package org.zotero.android.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
internal fun SettingsDivider() {
    Box(modifier = Modifier.background(CustomTheme.colors.surface)) {
        SidebarDivider(modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
internal fun SettingsSectionTitle(
    @StringRes titleId: Int
) {
    Text(
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        text = stringResource(id = titleId).uppercase(),
        fontSize = 14.sp,
        color = CustomTheme.colors.secondaryContent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}