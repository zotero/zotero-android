package org.zotero.android.screens.creatoredit.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.screens.creatoredit.CreatorEditViewState
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Strings

@Composable
internal fun CreatorEditRowFieldsBlock(
    viewState: CreatorEditViewState,
    viewModel: CreatorEditViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    lastNameFocusRequester: FocusRequester,
    fullNameFocusRequester: FocusRequester,
) {
    CreatorEditFieldTappableRow(
        detailTitle = stringResource(id = Strings.creator_editor_creator),
        detailValue = viewState.creator?.localizedType ?: "",
        layoutType = layoutType,
        onClick = viewModel::onCreatorTypeClicked
    )
    NewSettingsDivider()
    if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.separate) {
        CreatorEditFieldEditableRow(
            viewModel = viewModel,
            detailTitle = stringResource(id = Strings.creator_editor_last_name),
            detailValue = viewState.creator.lastName,
            onValueChange = viewModel::onLastNameChange,
            layoutType = layoutType,
            isLastField = false,
            focusRequester = lastNameFocusRequester,
        )
        CreatorEditFieldEditableRow(
            viewModel = viewModel,
            detailTitle = stringResource(id = Strings.creator_editor_first_name),
            detailValue = viewState.creator.firstName,
            onValueChange = viewModel::onFirstNameChange,
            layoutType = layoutType,
            isLastField = true,
        )
    } else {
        CreatorEditFieldEditableRow(
            viewModel = viewModel,
            detailTitle = stringResource(id = Strings.name),
            detailValue = viewState.creator?.fullName ?: "",
            onValueChange = viewModel::onFullNameChange,
            layoutType = layoutType,
            isLastField = true,
            focusRequester = fullNameFocusRequester,
        )
    }
}