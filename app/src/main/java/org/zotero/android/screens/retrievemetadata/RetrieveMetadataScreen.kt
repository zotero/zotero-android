package org.zotero.android.screens.retrievemetadata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataViewEffect.NavigateBack
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.screens.retrievemetadata.rows.RetrieveMetadataItemRow
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun RetrieveMetadataScreen(
    onBack: () -> Unit,
    viewModel: RetrieveMetadataViewModel = hiltViewModel(),
) {

    val viewState by viewModel.viewStates.observeAsState(RetrieveMetadataViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            NavigateBack -> {
                onBack()
            }
            null -> Unit
        }
    }

    CustomThemeWithStatusAndNavBars {
        val isLoadingState = viewState.retrieveMetadataState == RetrieveMetadataState.loading
        CustomScaffold(
            topBarColor = CustomTheme.colors.topBarBackgroundColor,
            topBar = {
                RetrieveMetadataTopBar(
                    onDone = {
                    onBack()
                },
                    onCancel = {
                        onBack()
                    }, isLoadingState = isLoadingState)
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CustomTheme.colors.popupBackgroundContent)
            ) {
                item {
                    RetrieveMetadataItemRow(viewState = viewState, showBottomDivider = false)
                }
            }
        }
    }
}

@Composable
private fun RetrieveMetadataDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    NewDivider()
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RetrieveMetadataTopBar(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    isLoadingState: Boolean,
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.retrieve_metadata_dialog_title),
        rightGuidelineStartPercentage = 0.2f,
        leftGuidelineStartPercentage = 0.2f,
        leftContainerContent = listOf {
            if (isLoadingState) {
                NewHeadingTextButton(
                    text = stringResource(id = Strings.cancel),
                    onClick = onCancel
                )
            }
        },
        rightContainerContent = listOf {
            if (!isLoadingState) {
                NewHeadingTextButton(
                    text = stringResource(id = Strings.done),
                    onClick = onDone
                )
            }

        }
    )
}