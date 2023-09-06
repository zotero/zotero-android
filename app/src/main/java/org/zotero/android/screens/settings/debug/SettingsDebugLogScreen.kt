package org.zotero.android.screens.settings.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.SidebarDivider
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
internal fun SettingsDebugLogScreen(
    onBack: () -> Unit,
    viewModel: SettingsDebugLogViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(SettingsDebugLogViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
           else -> {

           }
        }
    }
    CustomScaffold(
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
        topBar = {
            TopBar(
                onBack = onBack,
                numberOfLines = viewState.numberOfLines
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface)
        ) {
            item {
                SidebarDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = viewState.log,
                    fontSize = layoutType.calculateItemsRowTextSize(),
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    numberOfLines: Int,
) {
    CancelSaveTitleTopBar(
        title = quantityStringResource(id = Plurals.settings_lines, numberOfLines),
        onBack = onBack,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
    )
}