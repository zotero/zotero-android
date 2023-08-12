package org.zotero.android.screens.collectionpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
internal fun CollectionPickerScreen(
    onBack: () -> Unit,
    scaffoldModifier: Modifier = Modifier,
    viewModel: CollectionPickerViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CollectionPickerViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            is CollectionPickerViewEffect.OnBack -> {
                onBack()
            }
            else -> {
                //no-op
            }
        }
    }
    CustomScaffold(
        modifier = scaffoldModifier,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
        topBar = {
            TopBar(
                onCancelClicked = onBack,
                onAdd = viewModel::confirmSelection,
                viewState = viewState,
                viewModel = viewModel,
            )
        },
    ) {
        Column {
            CustomDivider()
            CollectionsPickerTable(viewState = viewState, viewModel = viewModel, layoutType = layoutType)
        }
    }
}

@Composable
private fun TopBar(
    onCancelClicked: () -> Unit,
    onAdd: () -> Unit,
    viewState: CollectionPickerViewState,
    viewModel: CollectionPickerViewModel
) {
    CancelSaveTitleTopBar(
        title = viewState.title,
        onCancel = onCancelClicked,
        onAdd = if (viewModel.multipleSelectionAllowed) onAdd else null,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
    )
}