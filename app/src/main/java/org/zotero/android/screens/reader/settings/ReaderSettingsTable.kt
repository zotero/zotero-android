package org.zotero.android.screens.reader.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.uicomponents.Strings
import java.util.Locale
import kotlin.math.roundToInt

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
        if (viewState.fileType == ReaderFileType.PDF && !viewState.readingModeEnabled) {
            readerSettingsSettingRow(
                titleResId = Strings.pdf_settings_scroll_mode_title,
                options = viewState.scrollModeOptions,
                selectedOption = viewState.selectedScrollModeOption,
                optionSelected = viewModel::onOptionSelected
            )
        }
        if ((viewState.fileType == ReaderFileType.PDF && !viewState.readingModeEnabled) || viewState.fileType == ReaderFileType.EPUB) {
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

        if (viewState.fileType == ReaderFileType.EPUB || viewState.readingModeEnabled) {
            readerSettingsSliderRow(
                titleResId = Strings.pdf_settings_line_height_title,
                value = viewState.lineHeight,
                valueRange = 0.80f..2.00f,
                steps = 2,
                valueLabel = String.format(Locale.getDefault(), "%.1f", viewState.lineHeight),
                onValueChange = viewModel::onLineHeightChanged,
            )
            readerSettingsSliderRow(
                titleResId = Strings.pdf_settings_word_spacing_title,
                value = viewState.wordSpacing,
                valueRange = -100f..100f,
                steps = 9,
                valueLabel = "${viewState.wordSpacing.roundToInt()}%",
                onValueChange = viewModel::onWordSpacingChanged,
            )
            readerSettingsSliderRow(
                titleResId = Strings.pdf_settings_letter_spacing_title,
                value = viewState.letterSpacing,
                valueRange = -0.1f..0.1f,
                steps = 9,
                valueLabel = "${(viewState.letterSpacing * 1000).roundToInt()}%",
                onValueChange = viewModel::onLetterSpacingChanged,
            )
            readerSettingsSliderRow(
                titleResId = Strings.pdf_settings_page_width_title,
                value = viewState.pageWidth,
                valueRange = -1f..1f,
                steps = 1,
                valueLabel = "${((viewState.pageWidth + 3) / 4 * 100).roundToInt()}%",
                onValueChange = viewModel::onPageWidthChanged,
            )
        }

    }
}
