package org.zotero.android.screens.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun FilterScreen(
    onBack: () -> Unit,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(FilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is FilterViewEffect.OnBack -> {
                onBack()
            }
        }
    }
    CustomScaffold(
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
        topBar = {
            TopBar(
                onDone = viewModel::onDone,
            )
        },
    ) {
        CustomDivider()
        Row(modifier = Modifier.padding(top = 16.dp, start = 16.dp)) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = Strings.downloaded_files),
                fontSize = layoutType.calculateItemsRowTextSize(),
                maxLines = 1,
            )
            CustomSwitch(
                checked = viewState.isDownloadsChecked,
                onCheckedChange = { viewModel.onDownloadsTapped()},
                modifier = Modifier
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun TopBar(
    onDone: () -> Unit,
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = Strings.filters),
        onDone = onDone,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
    )
}
