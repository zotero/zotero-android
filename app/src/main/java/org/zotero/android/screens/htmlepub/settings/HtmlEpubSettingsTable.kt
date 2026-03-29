package org.zotero.android.screens.htmlepub.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.uicomponents.Strings

@Composable
internal fun HtmlEpubSettingsTable(
    viewState: HtmlEpubSettingsViewState,
    viewModel: HtmlEpubSettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        htmlEpubSettingsSettingRow(
            titleResId = Strings.pdf_settings_appearance_title,
            options = viewState.appearanceOptions,
            selectedOption = viewState.selectedAppearanceOption,
            optionSelected = viewModel::onOptionSelected
        )
    }
}
