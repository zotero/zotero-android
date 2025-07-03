package org.zotero.android.screens.settings.cite

import androidx.compose.runtime.Composable
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.Strings


@Composable
internal fun SettingsCiteCitationStylesSection(
    viewState: SettingsCiteViewState,
    viewModel: SettingsCiteViewModel
) {
    SettingsSectionTitle(titleId = Strings.settings_cite_styles_title)
    SettingsSection {
        viewState.styles.forEach {
            SettingsItem(
                title = it.title,
                onItemTapped = {}
            )
            SettingsDivider()
        }

        SettingsDivider()
    }
}