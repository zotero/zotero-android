package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.rows.ItemDetailsAbstractFieldRow
import org.zotero.android.screens.itemdetails.rows.ItemDetailsDataRows
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun ItemDetailsViewScreen(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        item {
            Title(viewState)
            NewSettingsDivider()
        }
        item {
            ItemDetailsDataRows(viewState, layoutType, viewModel)

            if (!viewState.data.isAttachment && !viewState.data.abstract.isNullOrBlank()) {
                NewSettingsDivider()
                ItemDetailsAbstractFieldRow(
                    detailValue = viewState.data.abstract ?: "",
                )
            }
        }
        notesTagsAndAttachmentsBlock(
            viewState = viewState,
            viewModel = viewModel,
            layoutType = layoutType,
            onNoteClicked = { viewModel.openNoteEditor(it) },
            onAddNote = { viewModel.onAddNote() },
            onNoteLongClicked = viewModel::onNoteLongClick,
        )
    }
}

@Composable
private fun Title(
    viewState: ItemDetailsViewState,
) {
    SelectionContainer {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            text = viewState.data.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
        )
    }

}

