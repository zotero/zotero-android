package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CollectionEditFieldEditableRow(
    detailValue: String,
    textColor: Color = CustomTheme.colors.primaryContent,
    viewModel: CollectionEditViewModel,
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Spacer(modifier = Modifier.width(16.dp))
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = detailValue,
            maxLines = 1,
            singleLine = true,
            hint = stringResource(id = Strings.name),
            focusRequester = focusRequester,
            textColor = textColor,
            onValueChange = viewModel::onNameChanged,
            textStyle = CustomTheme.typography.newBody,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.onSave() }
            ),
            onEnterOrTab = { viewModel.onSave() }
        )
    }
}

@Composable
internal fun LibrarySelectorRow(
    viewState: CollectionEditViewState,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            )
            .clip(shape = RoundedCornerShape(10.dp))
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            modifier = Modifier
                .size(28.dp),
            painter = painterResource(
                id = if (viewState.parent == null) {
                    Drawables.icon_cell_library
                } else {
                    Drawables.icon_cell_collection
                }
            ),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroDefaultBlue
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            text = viewState.parent?.name ?: viewState.library.name,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            painter = painterResource(id = Drawables.chevron_right_24px),
            contentDescription = null,
            tint = CustomTheme.colors.chevronNavigationColor
        )
        Spacer(modifier = Modifier.width(8.dp))

    }
}