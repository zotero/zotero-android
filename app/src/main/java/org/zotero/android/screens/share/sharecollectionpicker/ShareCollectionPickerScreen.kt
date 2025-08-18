package org.zotero.android.screens.share.sharecollectionpicker

import androidx.compose.foundation.background
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
internal fun ShareCollectionPickerScreen(
    onBack: () -> Unit,
    viewModel: ShareCollectionPickerViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(ShareCollectionPickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is ShareCollectionPickerViewEffect.OnBack -> {
                    onBack()
                }

                else -> {
                    //no-op
                }
            }
        }
        CustomScaffold(
            topBarColor = CustomTheme.colors.topBarBackgroundColor,
            topBar = {
                ShareCollectionPickerTopBar(
                    onBackClicked = onBack,
                )
            },
        ) {
            Column(modifier = Modifier.background(CustomTheme.colors.popupBackgroundContent)) {
                ShareCollectionsPickerTable(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
            }
        }
    }
}