package org.zotero.android.screens.settings.cite

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.uicomponents.Strings


@Composable
internal fun SettingsCiteCitationStylesSection(
    viewState: SettingsCiteViewState,
    viewModel: SettingsCiteViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        viewState.styles.forEach { style ->
            item {
                NewSettingsItem(
                    title = style.title,
                    onItemTapped = {},
                    onItemLongTapped = { viewModel.onItemLongTapped(style) }
                )
            }

        }
        item {
            NewSettingsItem(
                textColor = MaterialTheme.colorScheme.primary,
                title = stringResource(id = Strings.settings_cite_get_more_styles),
                onItemTapped = viewModel::navigateToCiteSearch
            )
        }
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}