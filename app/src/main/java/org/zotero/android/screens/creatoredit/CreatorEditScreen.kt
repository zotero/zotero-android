package org.zotero.android.screens.creatoredit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun CreatorEditScreen(
    onBack: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    scaffoldModifier: Modifier,
    viewModel: CreatorEditViewModel = hiltViewModel(),
) {
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
        modifier = scaffoldModifier,
        topBar = {
            TopBar(
                onCloseClicked = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .background(color = CustomTheme.colors.surface)
                .padding(start = 16.dp),
        ) {
            displayFields(
                viewState = viewState,
                layoutType = layoutType,
                viewModel = viewModel,
                lastNameFocusRequester = lastNameFocusRequester,
                fullNameFocusRequester = fullNameFocusRequester
            )
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = viewModel::toggleNamePresentation
                        ),
                    text = stringResource(
                        id = if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.full)
                            Strings.switch_to_two_field else Strings.switch_to_single_field
                    ),
                    color = CustomTheme.colors.zoteroBlueWithDarkMode,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomDivider()
            }
        }
    }
}

private fun LazyListScope.displayFields(
    viewState: CreatorEditViewState,
    viewModel: CreatorEditViewModel,
    layoutType: LayoutType,
    lastNameFocusRequester: FocusRequester,
    fullNameFocusRequester: FocusRequester,
) {
    item {
        Spacer(modifier = Modifier.height(20.dp))
        CustomDivider()
        FieldTappableRow(
            detailTitle = stringResource(id = Strings.creator_type),
            detailValue = viewState.creator?.localizedType ?: "",
            layoutType = layoutType,
            onClick = viewModel::onCreatorTypeClicked
        )
        Spacer(modifier = Modifier.height(16.dp))
        CustomDivider()
    }

    if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.separate) {
        item {
            FieldEditableRow(
                viewModel = viewModel,
                detailTitle = stringResource(id = Strings.last_name),
                detailValue = viewState.creator?.lastName ?: "",
                onValueChange = viewModel::onLastNameChange,
                layoutType = layoutType,
                isLastField = false,
                focusRequester = lastNameFocusRequester,
            )
        }
        item {
            FieldEditableRow(
                viewModel = viewModel,
                detailTitle = stringResource(id = Strings.first_name),
                detailValue = viewState.creator?.firstName ?: "",
                onValueChange = viewModel::onFirstNameChange,
                layoutType = layoutType,
                isLastField = true,
            )
        }
    } else {
        item {
            FieldEditableRow(
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
}

@Composable
private fun FieldEditableRow(
    detailTitle: String,
    detailValue: String,
    viewModel: CreatorEditViewModel,
    layoutType: LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
    isLastField: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Column(
                modifier = Modifier
                    .width(layoutType.calculateItemFieldLabelWidth())
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = detailTitle,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                if (isLastField) {
                    CustomTextField(
                        modifier = Modifier
                            .fillMaxSize(),
                        value = detailValue,
                        hint = "",
                        textColor = textColor,
                        onValueChange = onValueChange,
                        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.onSave() }
                        ),
                        onEnterOrTab = { viewModel.onSave() },
                        focusRequester = focusRequester,
                    )
                } else {
                    val focusManager = LocalFocusManager.current
                    val moveFocusDownAction = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                    CustomTextField(
                        modifier = Modifier
                            .fillMaxSize(),
                        value = detailValue,
                        hint = "",
                        textColor = textColor,
                        onValueChange = onValueChange,
                        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
                        focusRequester = focusRequester,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { moveFocusDownAction() }
                        ),
                        onEnterOrTab = { moveFocusDownAction() },
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider()
    }
}

@Composable
private fun FieldTappableRow(
    detailTitle: String,
    detailValue: String,
    layoutType: LayoutType,
    onClick: () ->Unit,
    textColor: Color = CustomTheme.colors.primaryContent,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        ) {
            Column(
                modifier = Modifier
                    .width(layoutType.calculateItemFieldLabelWidth()),
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Start),
                    text = detailTitle,
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    modifier = Modifier,
                    text = detailValue,
                    color = textColor,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider()
    }
}


@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
    onSave: () -> Unit,
    viewState: CreatorEditViewState
) {
    CloseIconTopBar(
        title = viewState.creator?.localizedType,
        onClose = onCloseClicked,
        actions = {
            HeadingTextButton(
                onClick = onSave,
                text = stringResource(Strings.save),
                isEnabled = viewState.isValid
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}
