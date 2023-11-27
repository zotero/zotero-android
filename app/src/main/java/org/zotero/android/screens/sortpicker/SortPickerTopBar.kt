package org.zotero.android.screens.sortpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun SortPickerTopBar(
    onDone: () -> Unit,
) {
    NewCustomTopBar(
        rightContainerContent = listOf {
            NewHeadingTextButton(text = stringResource(id = Strings.done), onClick = onDone)
        },
    )
}
