package org.zotero.android.pdf.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.uicomponents.Strings

@Composable
internal fun PdfSettingsTable(
    viewState: PdfSettingsViewState,
    viewModel: PdfSettingsViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        pdfSettingsSettingRow(
            titleResId = Strings.pdf_settings_page_transition_title,
            options = viewState.pageTransitionOptions,
            selectedOption = viewState.selectedPageTransitionOption,
            optionSelected = viewModel::onOptionSelected
        )

        pdfSettingsSettingRow(
            titleResId = Strings.pdf_settings_page_mode_title,
            options = viewState.pageModeOptions,
            selectedOption = viewState.selectedPageModeOption,
            optionSelected = viewModel::onOptionSelected
        )

        pdfSettingsSettingRow(
            titleResId = Strings.pdf_settings_scroll_direction_title,
            options = viewState.scrollDirectionOptions,
            selectedOption = viewState.selectedScrollDirectionOption,
            optionSelected = viewModel::onOptionSelected
        )

        pdfSettingsSettingRow(
            titleResId = Strings.pdf_settings_page_fitting_title,
            options = viewState.pageFittingsOptions,
            selectedOption = viewState.selectedPageFittingOption,
            optionSelected = viewModel::onOptionSelected
        )

        pdfSettingsSettingRow(
            titleResId = Strings.pdf_settings_appearance_title,
            options = viewState.appearanceOptions,
            selectedOption = viewState.selectedAppearanceOption,
            optionSelected = viewModel::onOptionSelected
        )
    }
}
