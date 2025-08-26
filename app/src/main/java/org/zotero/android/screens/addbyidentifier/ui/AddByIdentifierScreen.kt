package org.zotero.android.screens.addbyidentifier.ui

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
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewEffect
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewModel
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewState
import org.zotero.android.screens.addbyidentifier.topbar.AddByIdentifierCloseAndCancelAllTopBar
import org.zotero.android.screens.addbyidentifier.topbar.AddByIdentifierTopBar
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun AddByIdentifierScreen(
    viewModel: AddByIdentifierViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    AppThemeM3 {
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
        CustomScaffoldM3(
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
