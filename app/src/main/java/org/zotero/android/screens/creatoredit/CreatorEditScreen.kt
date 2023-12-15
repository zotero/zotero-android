package org.zotero.android.screens.creatoredit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun CreatorEditScreen(
    onBack: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    viewModel: CreatorEditViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars {

        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(CreatorEditViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        val lastNameFocusRequester = remember { FocusRequester() }
        val fullNameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val effect = viewEffect?.consume()) {
                null -> Unit
                is CreatorEditViewEffect.OnBack -> {
                    onBack()
                }

                is CreatorEditViewEffect.NavigateToSinglePickerScreen -> {
                    navigateToSinglePickerScreen()
                }

                is CreatorEditViewEffect.RequestFocus -> {
                    when (effect.field) {
                        FocusField.FullName -> fullNameFocusRequester.requestFocus()
                        FocusField.LastName -> lastNameFocusRequester.requestFocus()
                    }
                }
            }
        }
        CustomScaffold(
            topBar = {
                CreatorEditTopBar(
                    onCloseClicked = onBack,
                    onSave = viewModel::onSave,
                    viewState = viewState,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.pdfAnnotationsFormBackground)
            ) {
                item {
                    CreatorEditRowFieldsBlock(
                        viewState = viewState,
                        layoutType = layoutType,
                        viewModel = viewModel,
                        lastNameFocusRequester = lastNameFocusRequester,
                        fullNameFocusRequester = fullNameFocusRequester
                    )
                    Box(modifier = Modifier.background(CustomTheme.colors.surface)) {
                        NewDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                    CreatorEditToggleNamePresentationRow(viewModel, viewState)
                    if (viewState.isEditing) {
                        CreatorEditSpacerBlock()
                        CreatorEditDeleteCreatorRow(viewModel, viewState)
                    } else {
                        NewDivider()
                    }
                }

            }
            if (viewState.shouldShowDeleteConfirmation) {
                CustomAlertDialog(
                    title = stringResource(id = Strings.warning),
                    description = stringResource(
                        id = Strings.creator_editor_delete_confirmation),
                    primaryAction = CustomAlertDialog.ActionConfig(
                        text = stringResource(id = Strings.cancel),
                        onClick = viewModel::onDismissDeleteConformation
                    ),
                    secondaryAction = CustomAlertDialog.ActionConfig(
                        text = stringResource(id = Strings.delete),
                        textColor = CustomPalette.ErrorRed,
                        onClick = { viewModel.deleteCreator() }
                    ),
                    onDismiss = viewModel::onDismissDeleteConformation
                )
            }
        }

    }
}