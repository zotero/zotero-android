package org.zotero.android.screens.addnote

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AddNoteScreen(
    onBack: () -> Unit,
    viewModel: AddNoteViewModel = hiltViewModel(),
    navigateToTagPicker: () -> Unit,
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(AddNoteViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        var isKeyboardShown by remember { mutableStateOf(false) }
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()

            KeyboardVisibilityEvent.setEventListener(
                context.findActivity()!!,
                lifecycleOwner
            ) { isOpen ->
                isKeyboardShown = isOpen
            }
        }

        BackHandler(
            enabled = viewState.backHandlerInterceptionEnabled,
            onBack = { viewModel.onDoneClicked() }
        )

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is AddNoteViewEffect.NavigateBack -> onBack()
                is AddNoteViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }

                AddNoteViewEffect.RefreshUI -> {}
            }
        }
        CustomScaffoldM3(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                AddNoteTopBar(titleData = viewState.title, onBack = viewModel::onDoneClicked)
            },
        ) {
            Box {
                AddNoteWebView(
                    viewModel = viewModel,
                    isKeyboardShown = isKeyboardShown
                )
                if (!isKeyboardShown) {
                    AddNoteTagSelector(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

}


private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}