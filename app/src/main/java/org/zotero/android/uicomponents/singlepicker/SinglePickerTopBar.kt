package org.zotero.android.uicomponents.singlepicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun SinglePickerTopBar(
    title: String?,
    onCancel: () -> Unit,
) {
    NewCustomTopBar(
        title = title,
        leftContainerContent = listOf {
            NewHeadingTextButton(text = stringResource(id = Strings.cancel), onClick = onCancel)
        }
    )
}
