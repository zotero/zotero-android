package org.zotero.android.screens.settings.csllocalepicker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.uicomponents.row.CustomRadioRow


@Composable
internal fun SettingsCslLocalePickerSections(
    viewState: SettingsCslLocalePickerViewState,
    viewModel: SettingsCslLocalePickerViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        viewState.locales.forEach { locale ->
            item {
                CustomRadioRow(
                    title = locale.name,
                    isSelected = locale.id == viewState.selected,
                    onItemTapped = { viewModel.onItemTapped(locale) },
                )
            }
        }
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}