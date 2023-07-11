package org.zotero.android.sample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.sample.MainViewModel
import org.zotero.android.sample.SampleViewEffect.NavigateBack
import org.zotero.android.sample.SampleViewState
import org.zotero.android.uicomponents.systemui.SolidStatusBar
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun SampleScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(SampleViewState())
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

    SolidStatusBar()

    CustomTextField(value = viewState.testText, onValueChange = {}, hint = "")
}
