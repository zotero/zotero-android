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
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.zotero.android.architecture.ui.ObserveLifecycleEvent
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig
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
        ObserveLifecycleEvent { event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    viewModel.onStop()
                }

                else -> {}
            }
        }
        var isLeaving by remember { mutableStateOf(false) }
        BackHandler(enabled = !isLeaving) { viewModel.onBackClicked() }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is AddNoteViewEffect.NavigateBack -> {
                    isLeaving = true
                    withFrameNanos { }
                    onBack()
                }
                is AddNoteViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
                AddNoteViewEffect.RefreshUI -> {}
            }
        }

        viewState.error?.let { error ->
            AddNoteErrorDialog(
                error = error,
                onDismiss = viewModel::onDismissError,
                onCloseWithoutSaving = viewModel::onCloseWithoutSaving,
            )
        }

        CustomScaffoldM3(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                AddNoteTopBar(
                    titleData = viewState.title,
                    isClosing = viewState.isClosing,
                    onBack = viewModel::onBackClicked,
                )
            },
        ) {
            Box {
                if (viewState.isEditorInstalled) {
                    key(viewState.webViewGeneration) {
                        AddNoteWebView(
                            viewModel = viewModel,
                            isKeyboardShown = isKeyboardShown
                        )
                    }
                }
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

@Composable
private fun AddNoteErrorDialog(
    error: AddNoteError,
    onDismiss: () -> Unit,
    onCloseWithoutSaving: () -> Unit,
) {
    if (error.isClosing) {
        CustomAlertDialogM3(
            title = stringResource(id = Strings.error),
            description = stringResource(id = Strings.errors_item_detail_cant_save_note),
            confirmButton = CustomAlertDialogM3ActionConfig(
                text = stringResource(id = Strings.close_without_saving),
                textColor = MaterialTheme.colorScheme.error,
                onClick = onCloseWithoutSaving,
            ),
            dismissButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.stay)),
            dismissOnClickOutside = false,
            onDismiss = onDismiss,
        )
    } else {
        CustomAlertDialogM3(
            title = stringResource(id = Strings.error),
            description = stringResource(id = Strings.errors_item_detail_cant_save_note),
            confirmButton = CustomAlertDialogM3ActionConfig(text = stringResource(id = Strings.ok)),
            onDismiss = onDismiss,
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}