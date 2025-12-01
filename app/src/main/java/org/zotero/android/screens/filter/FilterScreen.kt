package org.zotero.android.screens.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.filter.popup.FilterOptionsPopup

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
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                if (!isTablet) {
                    FilterTagsSearchBar(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            FilterTagsSearchBar(
                                horizontalPadding = 0.dp,
                                viewState = viewState,
                                viewModel = viewModel,
                            )
                        }

                        IconButton(
                            onClick = viewModel::onMoreSearchOptionsClicked
                        ) {
                            if (viewState.showFilterOptionsPopup) {
                                FilterOptionsPopup(
                                    viewState = viewState,
                                    viewModel = viewModel,
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Overflow",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }
            }
            filterTagsFlowTable(viewState = viewState, viewModel = viewModel)
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
            }
        }
        if (!isTablet) {
            DownloadFilesPart(viewState, viewModel)
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