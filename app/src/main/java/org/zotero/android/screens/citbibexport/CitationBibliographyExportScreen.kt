package org.zotero.android.screens.citbibexport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun CitationBibliographyExportScreen(
    onBack: () -> Unit,
    navigateToStylePicker: () -> Unit,
    navigateToCslLocalePicker: () -> Unit,
    viewModel: CitationBibliographyExportViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(CitationBibliographyExportViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val isTablet = layoutType.isTablet()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init(isTablet = isTablet)
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is CitationBibliographyExportViewEffect.OnBack -> {
                    onBack()
                }
                is CitationBibliographyExportViewEffect.NavigateToStylePicker -> {
                    navigateToStylePicker()
                }

                is CitationBibliographyExportViewEffect.NavigateToCslLocalePicker -> {
                    navigateToCslLocalePicker()
                }

                else -> {
                    //no-op
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                CitationBibliographyExportTopBar(
                    onCancel = onBack,
                    onDone = viewModel::onDone,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CitationBibliographyExportSections(
                    viewModel = viewModel,
                    viewState = viewState
                )
            }
        }
    }
}