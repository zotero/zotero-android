package org.zotero.android.screens.citbibexport

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMethod
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMode
import org.zotero.android.uicomponents.Strings


@Composable
internal fun CitBibExportSections(
    viewState: CitBibExportViewState,
    viewModel: CitBibExportViewModel,
) {
    CitBibExportItemWithDescription(
        title = stringResource(Strings.citation_style),
        description = viewState.style.title,
        onItemTapped = viewModel::onStyleTapped
    )

    CitBibExportItemWithDescription(
        title = stringResource(Strings.citation_language),
        description = viewState.localeName,
        isEnabled = viewState.languagePickerEnabled,
        onItemTapped = viewModel::onLanguageTapped
    )

    if (viewState.showOutputModeDialog) {
        CitBibExportOutputModeOptionsDialog(viewModel = viewModel, viewState = viewState)
    }
    CitBibExportItemWithDescription(
        title = stringResource(id = Strings.citation_output_mode),
        description =
            if (viewState.mode == CitBibExportOutputMode.bibliography) {
                stringResource( Strings.citation_bibliography)
            } else {
                citationTitle(viewState.style)
            }
        ,
        onItemTapped = viewModel::showOutputModeDialog
    )

    if (viewState.showOutputMethodDialog) {
        CitBibExportOutputMethodOptionsDialog(viewModel = viewModel, viewState = viewState)
    }
    CitBibExportItemWithDescription(
        title = stringResource(id = Strings.citation_output_method),
        description = stringResource(
            id = if (viewState.method == CitBibExportOutputMethod.copy) {
                Strings.citation_copy
            } else {
                Strings.citation_save_html
            }
        ),
        onItemTapped = viewModel::showOutputMethodDialog
    )
}