package org.zotero.android.screens.tagpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun TagPickerTopBar(
    onCancelClicked: () -> Unit,
    onSave: () -> Unit,
    viewState: TagPickerViewState
) {
    NewCustomTopBar(
        shouldAddBottomDivider = false,
        title = stringResource(
            id = Strings.tag_picker_title,
            viewState.selectedTags.size
        ),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.cancel),
                onClick = onCancelClicked
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(text = stringResource(id = Strings.save), onClick = onSave)
        }
    )
}