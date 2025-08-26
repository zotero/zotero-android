package org.zotero.android.screens.retrievemetadata

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataViewEffect.NavigateBack
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.screens.retrievemetadata.rows.RetrieveMetadataItemRow
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

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

    AppThemeM3 {
        val isLoadingState = viewState.retrieveMetadataState == RetrieveMetadataState.loading
        CustomScaffoldM3(
            topBar = {
                RetrieveMetadataTopBar(
                    onDone = {
                        onBack()
                    },
                    onCancel = {
                        onBack()
                    }, isLoadingState = isLoadingState
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    RetrieveMetadataItemRow(viewState = viewState)
                }
            }
        }
    }
}

