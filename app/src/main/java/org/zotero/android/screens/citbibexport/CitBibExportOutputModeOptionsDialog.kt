package org.zotero.android.screens.citbibexport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMode
import org.zotero.android.styles.data.Style
import org.zotero.android.uicomponents.Strings

@Composable
internal fun CitBibExportOutputModeOptionsDialog(
    viewModel: CitBibExportViewModel,
    viewState: CitBibExportViewState
) {
    Dialog(onDismissRequest = viewModel::dismissOutputModeDialog) {
        val roundCornerShape = RoundedCornerShape(size = 30.dp)
        Column(
            Modifier
                .wrapContentSize()
                .clip(roundCornerShape)
                .background(MaterialTheme.colorScheme.surface)
                .selectableGroup()
        ) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        text = stringResource(Strings.citation_output_mode),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
            )

            CitBibExportRadioButton(
                text = citationTitle(viewState.style),
                isSelected = viewState.mode == CitBibExportOutputMode.citation,
                onOptionSelected = { viewModel.setMode(CitBibExportOutputMode.citation) }
            )
            CitBibExportRadioButton(
                text = stringResource(Strings.citation_bibliography),
                isSelected = viewState.mode == CitBibExportOutputMode.bibliography,
                onOptionSelected = { viewModel.setMode(CitBibExportOutputMode.bibliography) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }


    }
}


@Composable
internal fun citationTitle(style: Style): String {
    return if (style.isNoteStyle) {
        stringResource(Strings.citation_notes)
    } else {
        stringResource(Strings.citation_citations)
    }
}