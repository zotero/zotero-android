package org.zotero.android.screens.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun FilterScreen(
    viewModel: FilterViewModel,
    viewState: FilterViewState,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomTheme.colors.popupBackgroundContent)
    ) {
        if (!isTablet) {
            DownloadFilesPart(viewState, viewModel)
        }
        LazyColumn {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                FilterTagsSearchRow(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            filterTagsFlowTable(viewState = viewState, viewModel = viewModel)
        }
    }
    val dialog = viewState.dialog
    if (dialog != null) {
        FilterDeleteAutomaticTagsDialog(
            filterDialog = dialog,
            onDismissDialog = viewModel::onDismissDialog,
            onDeleteAutomaticTags = { viewModel.deleteAutomaticTags() }
        )
    }
}