package org.zotero.android.screens.reader.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ReaderSettingsTable(
    viewState: ReaderSettingsViewState,
    viewModel: ReaderSettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        readerSettingsSettingRow(
            titleResId = Strings.pdf_settings_appearance_title,
            options = viewState.appearanceOptions,
            selectedOption = viewState.selectedAppearanceOption,
            optionSelected = viewModel::onOptionSelected
        )
        readerSettingsSettingRow(
            titleResId = Strings.pdf_settings_spreads_title,
            options = viewState.spreadsOptions,
            selectedOption = viewState.selectedSpreadsOption,
            optionSelected = viewModel::onOptionSelected
        )
        if (viewState.fileType != ReaderFileType.PDF) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_page_layout_title,
                options = viewState.pageLayoutFlowOptions,
                selectedOption = viewState.selectedPageLayoutFlowMode,
                optionSelected = viewModel::onOptionSelected
            )
        }

    }
}
