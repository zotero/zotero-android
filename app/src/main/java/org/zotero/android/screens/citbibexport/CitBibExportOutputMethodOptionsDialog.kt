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
import org.zotero.android.screens.citbibexport.data.CitBibExportOutputMethod
import org.zotero.android.uicomponents.Strings

@Composable
internal fun CitBibExportOutputMethodOptionsDialog(
    viewModel: CitBibExportViewModel,
    viewState: CitBibExportViewState
) {
    Dialog(onDismissRequest = viewModel::dismissOutputMethodDialog) {
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
                        text = stringResource(Strings.citation_output_method),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
            )

            CitBibExportRadioButton(
                text = stringResource(Strings.citation_copy),
                isSelected = viewState.method == CitBibExportOutputMethod.copy,
                onOptionSelected = { viewModel.setMethod(CitBibExportOutputMethod.copy) }
            )
            CitBibExportRadioButton(
                text = stringResource(Strings.citation_save_html),
                isSelected = viewState.method == CitBibExportOutputMethod.html,
                onOptionSelected = { viewModel.setMethod(CitBibExportOutputMethod.html) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
