package org.zotero.android.screens.retrievemetadata

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataViewEffect.NavigateBack
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun RetrieveMetadataScreen(
    viewModel: RetrieveMetadataViewModel = hiltViewModel(),
) {

    val viewState by viewModel.viewStates.observeAsState(RetrieveMetadataViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            NavigateBack -> {
                backDispatcher?.onBackPressed()
            }
            null -> Unit
        }
    }

    val isBackEnabled = viewState.retrieveMetadataState != RetrieveMetadataState.loading

    val backgroundColor = CustomTheme.colors.popupBackgroundContent

    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = backgroundColor
    ) {

        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                RetrieveMetadataTopBar(onDone = {
                    backDispatcher?.onBackPressed()
                }, isEnabled = isBackEnabled)
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                val retrieveMetadataState = viewState.retrieveMetadataState
                item {
                    FileNameRow(viewState.fileName)
                    RetrieveMetadataDivider()
                    StatusRow(retrieveMetadataState)
                    RetrieveMetadataDivider()
                }

                item {
                    when (retrieveMetadataState) {
                        is RetrieveMetadataState.success -> {
                        }

                        else -> {
                            //no-op
                        }
                    }
                }
            }
            if (viewState.retrieveMetadataState == RetrieveMetadataState.loading) {
                LoadingIndicator()
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
private fun StatusRow(state: RetrieveMetadataState) {
    val text = when(state) {
        is RetrieveMetadataState.failed -> {
            stringResource(Strings.retrieve_metadata_status_failed, state.message)
        }
        RetrieveMetadataState.loading -> {
            stringResource(Strings.retrieve_metadata_status_in_progress)
        }
        is RetrieveMetadataState.success -> {
            stringResource(Strings.retrieve_metadata_status_in_success)
        }
        is RetrieveMetadataState.recognizedDataIsEmpty -> {
            stringResource(Strings.retrieve_metadata_status_no_results)
        }

        RetrieveMetadataState.fileIsNotPdf -> {
            //no-op
            ""
        }
    }
    Text(
        modifier = Modifier
            .fillMaxSize(),
        text = text,
        color = CustomTheme.colors.primaryContent,
        style = CustomTheme.typography.default,
    )
    if (state is RetrieveMetadataState.failed) {
        Text(
            modifier = Modifier
                .fillMaxSize(),
            text = "Error: ${state.message}",
            color = CustomPalette.ErrorRed,
            style = CustomTheme.typography.default,
        )
    }
}

@Composable
private fun FileNameRow(fileName: String) {
    Text(
        modifier = Modifier
            .fillMaxSize(),
        text = stringResource(id = Strings.retrieve_metadata_filename, fileName),
        color = CustomTheme.colors.primaryContent,
        style = CustomTheme.typography.default,
    )
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            color = CustomTheme.colors.zoteroDefaultBlue,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        )
    }
}

@Composable
private fun RetrieveMetadataTopBar(
    onDone: () -> Unit,
    isEnabled: Boolean,
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.retrieve_metadata_dialog_title),
        rightGuidelineStartPercentage = 0.2f,
        leftGuidelineStartPercentage = 0.2f,
        rightContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.done),
                isEnabled = isEnabled,
                onClick = {
                    if (isEnabled) {
                        onDone()
                    }
                }
            )
        }
    )
}