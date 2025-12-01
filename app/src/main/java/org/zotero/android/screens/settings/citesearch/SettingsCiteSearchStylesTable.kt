package org.zotero.android.screens.settings.citesearch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsCiteSearchStylesTable(
    viewState: SettingsCiteSearchViewState,
    viewModel: SettingsCiteSearchViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val styles = viewState.filtered ?: viewState.styles
        items(styles) { style ->
            SettingsCiteSearchItem(
                title = style.title,
                onItemTapped = { viewModel.onItemTapped(style) },
            )
        }
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}

@Composable
internal fun SettingsCiteSearchItem(
    title: String,
    onItemTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onItemTapped() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
