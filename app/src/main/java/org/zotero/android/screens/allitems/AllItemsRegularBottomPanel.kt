package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.downloadedfiles.DownloadedFilesPopup
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun AllItemsRegularBottomPanel(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
) {
    FlexibleBottomAppBar(
        horizontalArrangement = Arrangement.SpaceBetween,
        content = {
            IconButton(onClick = { viewModel.showFilters() }) {
                Box {
                    if (viewState.showDownloadedFilesPopup) {
                        DownloadedFilesPopup(
                            viewState = viewState,
                            viewModel = viewModel,
                        )
                    }

                    val filterDrawable =
                        if (viewState.filters.isEmpty()) {
                            Drawables.filter_list_off_24px
                        } else {
                            Drawables.filter_list_24px
                        }
                    Icon(
                        painter = painterResource(filterDrawable),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FilledIconButton(
                modifier = Modifier.width(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                onClick = viewModel::onAdd,
            ) {
                Icon(
                    painter = painterResource(Drawables.add_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            IconButton(onClick = { viewModel.showSortPicker() }) {
                Icon(
                    painter = painterResource(Drawables.swap_vert_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        },
    )
}
