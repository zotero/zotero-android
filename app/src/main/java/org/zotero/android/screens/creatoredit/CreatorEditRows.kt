package org.zotero.android.screens.creatoredit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CreatorEditFieldTappableRow(
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    onClick: () -> Unit,
    textColor: Color = CustomTheme.colors.primaryContent,
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .width(layoutType.calculateItemFieldLabelWidth()),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start),
                text = detailTitle,
                color = CustomTheme.colors.pdfSizePickerColor,
                style = CustomTheme.typography.newBody,
            )
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = detailValue,
            color = textColor,
            style = CustomTheme.typography.newBody,
        )
        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(id = Drawables.chevron_right_24px),
            contentDescription = null,
            tint = CustomTheme.colors.chevronNavigationColor
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
internal fun CreatorEditFieldEditableRow(
    detailTitle: String,
    detailValue: String,
    viewModel: CreatorEditViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
    isLastField: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(CustomTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier
                .width(layoutType.calculateItemFieldLabelWidth())
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start),
                text = detailTitle,
                color = CustomTheme.colors.pdfSizePickerColor,
                style = CustomTheme.typography.newBody,
            )
        }

        if (isLastField) {
            CustomTextField(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
                value = detailValue,
                hint = "",
                textColor = textColor,
                onValueChange = onValueChange,
                maxLines = 1,
                singleLine = true,
                textStyle = CustomTheme.typography.newBody,
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
                    .padding(start = 12.dp)
                    .fillMaxWidth(),
                value = detailValue,
                hint = "",
                textColor = textColor,
                onValueChange = onValueChange,
                maxLines = 1,
                singleLine = true,
                textStyle = CustomTheme.typography.newBody,
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
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
internal fun CreatorEditToggleNamePresentationRow(
    viewModel: CreatorEditViewModel,
    viewState: CreatorEditViewState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = viewModel::toggleNamePresentation
            )
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = stringResource(
                id = if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.full)
                    Strings.creator_editor_switch_to_dual else Strings.creator_editor_switch_to_single
            ),
            color = CustomTheme.colors.zoteroDefaultBlue,
            style = CustomTheme.typography.newBody,
        )
    }
}

@Composable
internal fun CreatorEditDeleteCreatorRow(
    viewModel: CreatorEditViewModel,
    viewState: CreatorEditViewState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = viewModel::showDeleteCreatorConfirmation
            )
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            text = stringResource(
                id = Strings.delete
            ) + " " + viewState.creator?.localizedType,
            color = Color(0xFFDB2C3A),
            style = CustomTheme.typography.newBody,
        )
    }
}