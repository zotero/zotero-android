package org.zotero.android.screens.settings.citesearch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsCiteSearchStylesTable(
    viewState: SettingsCiteSearchViewState,
    viewModel: SettingsCiteSearchViewModel
) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
        ) {
            val styles = viewState.filtered ?: viewState.styles
            itemsIndexed(items = styles) { index, style ->
                SettingsCiteSearchItem(
                    title = style.title,
                    onItemTapped = { viewModel.onItemTapped(style) },
                )
                if (index != viewState.styles.size - 1) {
                    SettingsDivider()
                }
            }
    }
}

@Composable
internal fun SettingsCiteSearchItem(
    title: String,
    textColor: Color = CustomTheme.colors.primaryContent,
    onItemTapped: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onItemTapped() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier.padding(vertical = 10.dp).padding(end = 8.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = textColor,
        )
    }
}
