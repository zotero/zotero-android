package org.zotero.android.screens.creatoredit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CreatorEditRowFieldsBlock(
    viewState: CreatorEditViewState,
    viewModel: CreatorEditViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    lastNameFocusRequester: FocusRequester,
    fullNameFocusRequester: FocusRequester,
) {
    CreatorEditSpacerBlock()
    CreatorEditFieldTappableRow(
        detailTitle = stringResource(id = Strings.creator_editor_creator),
        detailValue = viewState.creator?.localizedType ?: "",
        layoutType = layoutType,
        onClick = viewModel::onCreatorTypeClicked
    )
    CreatorEditSpacerBlock()
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
        Box(modifier = Modifier.background(CustomTheme.colors.surface)) {
            NewDivider(modifier = Modifier.padding(start = 16.dp))
        }
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

@Composable
internal fun CreatorEditSpacerBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(CustomTheme.colors.zoteroItemDetailSectionBackground)
    ) {
        NewDivider(modifier = Modifier.align(Alignment.TopStart))
        NewDivider(modifier = Modifier.align(Alignment.BottomStart))
    }
}