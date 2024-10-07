package org.zotero.android.screens.addbyidentifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewEffect
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewModel
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewState
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun AddByIdentifierScreen(
    viewModel: AddByIdentifierViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    ) {
        val viewState by viewModel.viewStates.observeAsState(AddByIdentifierViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is AddByIdentifierViewEffect.NavigateBack -> {
                    onClose()
                }

                else -> {}
            }
        }
        CustomScaffold(
            topBar = {
                when (viewState.lookupState) {
                    AddByIdentifierViewModel.State.loadingIdentifiers,
                    is AddByIdentifierViewModel.State.lookup -> {
                        AddByIdentifierCloseAndCancelAllTopBar(
                            onClose = onClose,
                            onCancelAll = {
                                viewModel.cancelAllLookups()
                                onClose()
                            }
                        )
                    }

                    else -> {
                        AddByIdentifierTopBar(
                            title = null,
                            onCancel = onClose,
                            onLookup = viewModel::onLookup
                        )
                    }
                }

            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                    .padding(horizontal = 16.dp)
            ) {
                when (viewState.lookupState) {
                    AddByIdentifierViewModel.State.loadingIdentifiers -> {
                        addByIdentifierLoadingIndicator()
                    }

                    is AddByIdentifierViewModel.State.lookup -> {
                        addByIdentifierTable(rows = viewState.lookupRows)
                    }

                    else -> {
                        addByIdentifierTitleEditFieldAndError(
                            viewState = viewState,
                            viewModel = viewModel,
                            failedState = viewState.lookupState as? AddByIdentifierViewModel.State.failed
                        )
                    }
                }
            }
        }
    }
}
