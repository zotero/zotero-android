package org.zotero.android.screens.settings.pageturning

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.pageturning.SettingsButtonPageTurningSwitchItem
import org.zotero.android.screens.settings.pageturning.SettingsButtonKeepZoomSwitchItem
import org.zotero.android.uicomponents.Strings


@Composable
internal fun SettingsPageTurningSections(
    buttonPageTurning: Boolean,
    onButtonPageTurningSwitchTapped: (Boolean) -> Unit,
    buttonKeepZoom: Boolean,
    onButtonKeepZoomSwitchTapped: (Boolean) -> Unit,
) {
    SettingsButtonPageTurningSwitchItem(
        title = stringResource(Strings.settings_page_turning_button),
        isChecked = buttonPageTurning,
        onCheckedChange = onButtonPageTurningSwitchTapped
    )
    SettingsButtonKeepZoomSwitchItem(
        title = stringResource(Strings.settings_keep_zoom_button),
        isChecked = buttonKeepZoom,
        onCheckedChange = onButtonKeepZoomSwitchTapped
    )
}
