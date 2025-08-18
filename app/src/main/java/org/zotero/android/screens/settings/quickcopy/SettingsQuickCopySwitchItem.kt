package org.zotero.android.screens.settings.quickcopy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsQuickCopySwitchItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, end = 0.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
        CustomSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
        )
    }
}