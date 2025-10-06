package org.zotero.android.screens.creatoredit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.creatoredit.dialogs.CreatorEditCreatorTypeDialog
import org.zotero.android.screens.creatoredit.dialogs.CreatorEditDeleteCreatorDialog
import org.zotero.android.screens.creatoredit.rows.CreatorEditDeleteCreatorRow
import org.zotero.android.screens.creatoredit.rows.CreatorEditRowFieldsBlock
import org.zotero.android.screens.creatoredit.rows.CreatorEditToggleNamePresentationRow
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun CreatorEditScreen(
    onBack: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    viewModel: CreatorEditViewModel = hiltViewModel(),
) {
    AppThemeM3 {
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
        CustomScaffoldM3(
            topBar = {
                CreatorEditTopBar(
                    onBack = onBack,
                    onSave = viewModel::onSave,
                    viewState = viewState,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    CreatorEditRowFieldsBlock(
                        viewState = viewState,
                        viewModel = viewModel,
                        layoutType = layoutType,
                        lastNameFocusRequester = lastNameFocusRequester,
                        fullNameFocusRequester = fullNameFocusRequester
                    )
                    CreatorEditToggleNamePresentationRow(viewModel, viewState)
                    if (viewState.isEditing) {
                        NewSettingsDivider()
                        CreatorEditDeleteCreatorRow(viewModel, viewState)
                    }
                }

            }
            if (viewState.shouldShowDeleteConfirmation) {
                CreatorEditDeleteCreatorDialog(viewModel = viewModel)
            }
            if (viewState.showChooserDialog) {
                CreatorEditCreatorTypeDialog(viewModel = viewModel, viewState = viewState)
            }
        }

    }
}