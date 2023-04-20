package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.screens.collectionedit.data.CollectionEditError
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun CollectionEditScreen(
    onBack: () -> Unit,
    navigateToCollectionPickerScreen: () -> Unit,
    scaffoldModifier: Modifier,
    viewModel: CollectionEditViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CollectionEditViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is CollectionEditViewEffect.OnBack -> {
                onBack()
            }
            is CollectionEditViewEffect.NavigateToCollectionPickerScreen -> {
                navigateToCollectionPickerScreen()
            }
        }
    }
    CustomScaffold(
        modifier = scaffoldModifier,
        topBar = {
            TopBar(
                onCancel = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                .padding(horizontal = 20.dp)
        ) {
            displayFields(
                viewState = viewState,
                layoutType = layoutType,
                viewModel = viewModel,
            )
        }
        val error = viewState.error
        if (error != null) {
            CollectionEditErrorDialogs(
                error = error,
                onDismissErrorDialog = viewModel::onDismissErrorDialog,
                deleteOrRestoreItem = viewModel::deleteOrRestoreCollection
            )
        }
    }
}

private fun LazyListScope.displayFields(
    viewState: CollectionEditViewState,
    viewModel: CollectionEditViewModel,
    layoutType: LayoutType
) {
    item {
        Spacer(modifier = Modifier.height(30.dp))
        FieldEditableRow(
            detailValue = viewState.name,
            viewModel  = viewModel,
            layoutType = layoutType
        )
    }
    item {
        Spacer(modifier = Modifier.height(30.dp))
        LibrarySelectorRow(
            viewState = viewState,
            layoutType = layoutType,
            onClick = viewModel::onParentClicked
        )
        if (viewState.key != null) {
            Spacer(modifier = Modifier.height(30.dp))
            FieldTappableRow(
                title = stringResource(id = Strings.delete_collection),
                layoutType = layoutType,
                onClick = viewModel::delete
            )
            Spacer(modifier = Modifier.height(10.dp))
            FieldTappableRow(
                title = stringResource(id = Strings.delete_collection_and_items),
                layoutType = layoutType,
                onClick = viewModel::deleteWithItems
            )
        }
    }

}

@Composable
private fun FieldEditableRow(
    detailValue: String,
    layoutType: LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    viewModel: CollectionEditViewModel,
) {
    Column(
        modifier = Modifier.background(
            color = CustomTheme.colors.zoteroEditFieldBackground,
            shape = RoundedCornerShape(size = 10.dp)
        )
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        CustomTextField(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp),
            value = detailValue,
            maxLines = 1,
            singleLine = true,
            hint = stringResource(id = Strings.name),
            focusRequester = focusRequester,
            textColor = textColor,
            onValueChange = viewModel::onNameChanged,
            textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.onSave() }
            ),
            onEnterOrTab = { viewModel.onSave() }
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun LibrarySelectorRow(
    viewState: CollectionEditViewState,
    layoutType: LayoutType,
    onClick: () ->Unit,
) {
    Column(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            )
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
//        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                modifier = Modifier
                    .size(layoutType.calculateIconSize()),
                painter = painterResource(
                    id = if (viewState.parent == null) {
                        Drawables.icon_cell_library
                    } else {
                        Drawables.icon_cell_collection
                    }
                ),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroBlueWithDarkMode
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                text = viewState.parent?.name ?: viewState.library.name,
                fontSize = layoutType.calculateTextSize(),
                color = CustomTheme.colors.primaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
//        Spacer(modifier = Modifier.height(5.dp))
    }

}


@Composable
private fun FieldTappableRow(
    title: String,
    layoutType: LayoutType,
    onClick: () ->Unit,
) {
    Column(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            )
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                text = title,
                fontSize = layoutType.calculateTextSize(),
                color = CustomTheme.colors.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
    }

}


@Composable
private fun TopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewState: CollectionEditViewState
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = if(viewState.key != null) Strings.edit_collection else Strings.create_collection),
        onCancel = onCancel,
        onSave = onSave,
        isSaveButtonEnabled = viewState.isValid
    )
}

@Composable
internal fun CollectionEditErrorDialogs(
    error: CollectionEditError,
    onDismissErrorDialog: () -> Unit,
    deleteOrRestoreItem: (isDelete: Boolean) -> Unit,
) {
    when (error) {
        is CollectionEditError.askUserToDeleteOrRestoreCollection -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.deletedTitle),
                description = stringResource(
                    id = Strings.collection_was_deleted,
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.yes),
                    onClick = { deleteOrRestoreItem(false) }
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.delete),
                    textColor = CustomPalette.ErrorRed,
                    onClick = { deleteOrRestoreItem(true) }
                ),
                onDismiss = onDismissErrorDialog
            )
        }
    }
}

