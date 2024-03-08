package org.zotero.android.screens.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.share.ShareViewEffect.NavigateBack

@Composable
internal fun ShareScreen(
    onBack: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(ShareViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            NavigateBack -> onBack()
            null -> Unit
        }
    }
}
