package org.zotero.android.screens.reader.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.settings.data.ReaderSettingsOptions
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
        if (viewState.fileType == ReaderFileType.PDF) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_scroll_mode_title,
                options = viewState.scrollModeOptions,
                selectedOption = viewState.selectedScrollModeOption,
                optionSelected = viewModel::onOptionSelected
            )
        }
        if (viewState.fileType == ReaderFileType.PDF) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_fit_to_content_title,
                options = viewState.fitToContentOptions,
                selectedOption = viewState.selectedFitToContentOption,
                optionSelected = viewModel::onOptionSelected
            )
            if (viewState.selectedFitToContentOption == ReaderSettingsOptions.FitToContentOn) {
                readerSettingsSettingRow(
                    titleResId = Strings.pdf_settings_content_margin_title,
                    options = viewState.contentMarginOptions,
                    selectedOption = viewState.selectedContentMarginOption,
                    optionSelected = viewModel::onOptionSelected
                )
            }
        }
        if (viewState.fileType == ReaderFileType.PDF || viewState.fileType == ReaderFileType.EPUB) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_spreads_title,
                options = viewState.spreadsOptions,
                selectedOption = viewState.selectedSpreadsOption,
                optionSelected = viewModel::onOptionSelected
            )
        }
        if (viewState.fileType == ReaderFileType.EPUB) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_page_layout_title,
                options = viewState.pageLayoutFlowOptions,
                selectedOption = viewState.selectedPageLayoutFlowMode,
                optionSelected = viewModel::onOptionSelected
            )
        }

    }
}
