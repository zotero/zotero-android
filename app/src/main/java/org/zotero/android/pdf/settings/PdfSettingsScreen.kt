package org.zotero.android.pdf.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.settings.PdfSettingsViewEffect.NavigateBack
import org.zotero.android.pdf.settings.data.PdfSettingsArgs
import org.zotero.android.pdf.settings.data.PdfSettingsOptions
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.selector.MultiSelector
import org.zotero.android.uicomponents.selector.MultiSelectorOption
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfSettingsScreen(
    args: PdfSettingsArgs,
    onBack: () -> Unit,
    viewModel: PdfSettingsViewModel = hiltViewModel(),
) {
    BackHandler(onBack = {
        onBack()
    })
    LaunchedEffect(args) {
        viewModel.init(args = args)
    }

    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfSettingsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(
        isDarkTheme = viewState.isDark,
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor
    ) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                NavigateBack -> onBack()
                null -> Unit
            }
        }
        CustomScaffold(
            topBar = {
                PdfSettingsTopBar(
                    onDone = onBack,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CustomTheme.colors.surface)
            ) {
                settingsRow(
                    titleResId = Strings.pdf_settings_page_transition_title,
                    options = viewState.pageTransitionOptions,
                    selectedOption = viewState.selectedPageTransitionOption,
                    optionSelected = viewModel::onOptionSelected
                )

                settingsRow(
                    titleResId = Strings.pdf_settings_page_mode_title,
                    options = viewState.pageModeOptions,
                    selectedOption = viewState.selectedPageModeOption,
                    optionSelected = viewModel::onOptionSelected
                )

                settingsRow(
                    titleResId = Strings.pdf_settings_scroll_direction_title,
                    options = viewState.scrollDirectionOptions,
                    selectedOption = viewState.selectedScrollDirectionOption,
                    optionSelected = viewModel::onOptionSelected
                )

                settingsRow(
                    titleResId = Strings.pdf_settings_page_fitting_title,
                    options = viewState.pageFittingsOptions,
                    selectedOption = viewState.selectedPageFittingOption,
                    optionSelected = viewModel::onOptionSelected
                )

                settingsRow(
                    titleResId = Strings.pdf_settings_appearance_title,
                    options = viewState.appearanceOptions,
                    selectedOption = viewState.selectedAppearanceOption,
                    optionSelected = viewModel::onOptionSelected
                )
            }
        }
    }
}

private fun LazyListScope.settingsRow(
    @StringRes titleResId: Int,
    options: List<PdfSettingsOptions>,
    selectedOption: PdfSettingsOptions,
    optionSelected: (Int) -> Unit,
) {
    item {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val selectorOptions = options.map {
            MultiSelectorOption(
                id = it.ordinal, optionString = stringResource(id = it.optionStringId)
            )
        }
        Column(modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 0.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = CustomTheme.colors.primaryContent,
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = titleResId),
                    fontSize = layoutType.calculateItemsRowTextSize(),
                    maxLines = 1,
                )
                val selectorColor = CustomTheme.colors.primaryContent
                MultiSelector(
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .width(240.dp)
                        .height(layoutType.calculateSelectorHeight()),
                    options = selectorOptions,
                    selectedOptionId = selectedOption.ordinal,
                    onOptionSelect = optionSelected,
                    fontSize = layoutType.calculatePdfSettingsOptionTextSize(),
                    selectedColor = selectorColor,
                    unselectedcolor = selectorColor
                )
            }
        }
        CustomDivider()
    }
}

