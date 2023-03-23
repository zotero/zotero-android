package org.zotero.android.screens.collections

import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CollectionsTopBar(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel
) {
    TopAppBar(
        title = {
            Text(
                text = viewState.library.name,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2
            )
        },
        navigationIcon = {
            //TODO add back shevron when collections are supported
        },
        actions = {

        },
        backgroundColor = CustomTheme.colors.surface,
    )
}
