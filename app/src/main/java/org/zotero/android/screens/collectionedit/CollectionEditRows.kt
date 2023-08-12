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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CollectionEditFieldEditableRow(
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
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
internal fun LibrarySelectorRow(
    viewState: CollectionEditViewState,
    layoutType: CustomLayoutSize.LayoutType,
    onClick: () -> Unit,
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
    }
}

@Composable
internal fun CollectionEditFieldTappableRow(
    title: String,
    layoutType: CustomLayoutSize.LayoutType,
    onClick: () -> Unit,
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