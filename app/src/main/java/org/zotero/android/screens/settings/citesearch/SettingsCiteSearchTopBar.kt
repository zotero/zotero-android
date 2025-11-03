package org.zotero.android.screens.settings.citesearch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun SettingsCiteSearchTopBar(
    onBack: () -> Unit,
    viewState: SettingsCiteSearchViewState,
    viewModel: SettingsCiteSearchViewModel
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
    ) {
        SettingsCiteAppSearchBarM3Wrapper(
            text = viewState.searchTerm,
            onValueChanged = { viewModel.onSearch(it) },
            onBack = {
                viewModel.onSearch("")
                onBack()
            }
        )
    }
}