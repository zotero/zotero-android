package org.zotero.android.screens.settings.cite

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme


@Composable
internal fun SettingsCiteCitationStylesSection(
    viewState: SettingsCiteViewState,
    viewModel: SettingsCiteViewModel
) {
    SettingsSectionTitle(titleId = Strings.settings_cite_styles_title)
    SettingsSection {
        viewState.styles.forEachIndexed { index, style ->
            SettingsItem(
                title = style.title,
                onItemTapped = {},
                onItemLongTapped = { viewModel.onItemLongTapped(style) }
            )
            if (index != viewState.styles.size - 1)
                SettingsDivider()
        }
    }
    Spacer(modifier = Modifier.height(30.dp))
    SettingsSection {
        SettingsItem(
            textColor = CustomTheme.colors.zoteroDefaultBlue,
            chevronNavigationColor = CustomTheme.colors.zoteroDefaultBlue,
            addNewScreenNavigationIndicator = true,
            title = stringResource(id = Strings.settings_cite_get_more_styles),
            onItemTapped = viewModel::navigateToCiteSearch
        )
    }
    Spacer(modifier = Modifier.height(30.dp))

}