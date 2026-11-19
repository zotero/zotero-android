package org.zotero.android.screens.settings.pageturning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.controls.CustomSwitch

@Composable
internal fun SettingsButtonPageTurningSwitchItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterStart)
                .padding(start = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        CustomSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterEnd)
                .padding(end = 16.dp),
        )
    }
}

@Composable
internal fun SettingsButtonKeepZoomSwitchItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterStart)
                .padding(start = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        CustomSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterEnd)
                .padding(end = 16.dp),
        )
    }
}