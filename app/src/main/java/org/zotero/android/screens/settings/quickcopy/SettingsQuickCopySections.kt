package org.zotero.android.screens.settings.quickcopy

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.elements.NewSettingsItemWithDescription
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
    NewSettingsItemWithDescription(
        title = stringResource(Strings.settings_export_default_format),
        description = selectedStyle,
        onItemTapped = onDefaultFormatTapped
    )

    NewSettingsItemWithDescription(
        title = stringResource(Strings.settings_export_language),
        description = selectedLanguage,
        isEnabled = languagePickerEnabled,
        onItemTapped = onLanguageTapped
    )

    SettingsQuickCopySwitchItem(
        title = stringResource(Strings.settings_export_copy_as_html),
        isChecked = copyAsHtml,
        onCheckedChange = onQuickCopySwitchTapped
    )
}