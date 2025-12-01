package org.zotero.android.screens.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.filter.popup.FilterOptionsPopup
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun FilterTopBar(
    onDone: () -> Unit,
    viewState: FilterViewState,
    viewModel: FilterViewModel,
    scrollBehavior: TopAppBarScrollBehavior
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Text(
                text = stringResource(
                    id = Strings.items_filters_title
                ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onDone) {
                Icon(
                    painter = painterResource(Drawables.ic_close_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onDone,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(Strings.done),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
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
    )
}