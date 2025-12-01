package org.zotero.android.screens.settings.stylepicker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.uicomponents.row.CustomRadioRow

@Composable
internal fun SettingsCiteCitationStylesSection(
    viewState: SettingsStylePickerViewState,
    viewModel: SettingsStylePickerViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        viewState.styles.forEach { style ->
            item {
                CustomRadioRow(
                    title = style.title,
                    isSelected = style.identifier == viewState.selected,
                    onItemTapped = { viewModel.onItemTapped(style) },
                )
            }

        }
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}

