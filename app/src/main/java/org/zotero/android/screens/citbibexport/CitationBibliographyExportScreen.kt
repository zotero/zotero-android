package org.zotero.android.screens.citbibexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun CitationBibliographyExportScreen(
    onBack: () -> Unit,
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
        }
    }
}