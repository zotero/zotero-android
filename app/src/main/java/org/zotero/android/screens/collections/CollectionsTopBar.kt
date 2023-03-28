package org.zotero.android.screens.collections

import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CollectionsTopBar(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = viewState.library.name,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2
            )
        },
        navigationIcon = {
        },
        actions = {
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}
