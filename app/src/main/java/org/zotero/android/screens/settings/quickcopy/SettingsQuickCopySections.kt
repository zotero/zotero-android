package org.zotero.android.screens.settings.quickcopy

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.Strings


@Composable
internal fun SettingsQuickCopySections(
    selectedStyle: String,
    selectedLanguage: String,
    languagePickerEnabled: Boolean,
    copyAsHtml: Boolean,
    onDefaultFormatTapped: () -> Unit,
    onLanguageTapped: () -> Unit,
    onQuickCopySwitchTapped: (Boolean) -> Unit,
) {
    SettingsSection {
        SettingsQuickCopyArrowItem(
            title = stringResource(Strings.settings_export_default_format),
            text = selectedStyle,
            onTapped = onDefaultFormatTapped
        )
        SettingsDivider()
        SettingsQuickCopyArrowItem(
            title = stringResource(Strings.settings_export_language),
            text = selectedLanguage,
            isEnabled = languagePickerEnabled,
            onTapped = onLanguageTapped
        )
        SettingsDivider()
        SettingsQuickCopySwitchItem(
            title = stringResource(Strings.settings_export_copy_as_html),
            isChecked = copyAsHtml,
            onCheckedChange = onQuickCopySwitchTapped
        )
    }
}