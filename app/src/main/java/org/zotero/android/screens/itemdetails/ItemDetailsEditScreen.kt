package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.rows.edit.itemDetailsEditDataRows
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.reorder.ReorderableState
import org.zotero.android.uicomponents.reorder.reorderable
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun ItemDetailsEditScreen(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
    reorderState: ReorderableState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .reorderable(
                state = reorderState,
                onMove = viewModel::onMove,
            ),
        state = reorderState.listState,
    ) {
        item {
            EditTitle(viewState, onValueChange = viewModel::onTitleEdit)
            NewSettingsDivider()
        }
        itemDetailsEditDataRows(viewState, viewModel, reorderState)
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}


@Composable
private fun EditTitle(
    viewState: ItemDetailsViewState,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val moveFocusDownAction = {
        focusManager.moveFocus(FocusDirection.Down)
    }
    CustomTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        value = viewState.data.title,
        hint = stringResource(id = Strings.item_detail_untitled),
        onValueChange = onValueChange,
        textColor = MaterialTheme.colorScheme.onSurface,
        textStyle = MaterialTheme.typography.headlineSmall,
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