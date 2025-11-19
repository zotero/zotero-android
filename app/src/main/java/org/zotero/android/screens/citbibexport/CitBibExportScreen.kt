package org.zotero.android.screens.citbibexport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.zotero.android.architecture.ui.ObserveLifecycleEvent
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3
import java.io.File

@Composable
internal fun CitBibExportScreen(
    onBack: () -> Unit,
    navigateToStylePicker: () -> Unit,
    navigateToCslLocalePicker: () -> Unit,
    onExportHtml: (file: File) -> Unit,
    viewModel: CitBibExportViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(CitBibExportViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        ObserveLifecycleEvent { event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.closeScreenIfNeeded()
                }

                else -> {}
            }
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                is CitBibExportViewEffect.OnBack -> {
                    onBack()
                }
                is CitBibExportViewEffect.NavigateToStylePicker -> {
                    navigateToStylePicker()
                }

                is CitBibExportViewEffect.NavigateToCslLocalePicker -> {
                    navigateToCslLocalePicker()
                }
                is CitBibExportViewEffect.ExportHtml -> {
                    onExportHtml(consumedEffect.file)
                }

                else -> {
                    //no-op
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                CitBibExportTopBar(
                    onCancel = onBack,
                    onDone = viewModel::process,
                    isDoneButtonEnabled = viewState.isDoneEnabled,
                    isLoading = viewState.isLoading
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CitBibExportSections(
                    viewModel = viewModel,
                    viewState = viewState
                )
            }
        }
    }
}