package org.zotero.android.screens.settings.quickcopy

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme


@Composable
internal fun SettingsQuickCopySection(
    viewState: SettingsQuickCopyViewState,
    viewModel: SettingsQuickCopyViewModel
) {
    SettingsSection {
        SettingsQuickCopyArrowItem(
            title = stringResource(Strings.settings_export_default_format),
            text = "American Psychological Association 7th edition",
            onTapped = viewModel::onDefaultFormatTapped
        )
        SettingsDivider()
        SettingsQuickCopyArrowItem(
            title = stringResource(Strings.settings_export_language),
            text = "English (English)",
            onTapped = viewModel::onLanguageTapped
        )
        SettingsDivider()
        SettingsQuickCopySwitchItem(
            title = stringResource(Strings.settings_export_copy_as_html),
            isChecked = true,
            onCheckedChange = viewModel::onQuickCopySwitchTapped
        )
    }
}

@Composable
private fun SettingsQuickCopyArrowItem(
    title: String,
    text: String,
    onTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                onClick = onTapped,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, end = 0.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp, start = 140.dp),
            text = text,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.chevronNavigationColor,
        )
        Icon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            painter = painterResource(id = Drawables.chevron_right_24px),
            contentDescription = null,
            tint = CustomTheme.colors.chevronNavigationColor
        )
    }
}

@Composable
private fun SettingsQuickCopySwitchItem(
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