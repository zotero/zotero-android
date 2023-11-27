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
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun CollectionPickerScreen(
    onBack: () -> Unit,
    scaffoldModifier: Modifier = Modifier,
    viewModel: CollectionPickerViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.popupBackgroundContent
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = backgroundColor
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
            backgroundColor = backgroundColor,
            topBar = {
                CollectionPickerTopBar(
                    onCancelClicked = onBack,
                    onAdd = viewModel::confirmSelection,
                    viewState = viewState,
                    viewModel = viewModel,
                )
            },
        ) {
            Column {
                CollectionsPickerTable(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
            }
        }
    }
}