package org.zotero.android.screens.mediaviewer.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ImageViewerTopBar(
    title: String,
    onDoneClicked: () -> Unit,
) {
    NewCustomTopBar(
        title = title,
        rightContainerContent = listOf {
            NewHeadingTextButton(
                isEnabled = true,
                onClick = onDoneClicked,
                text = stringResource(Strings.done)
            )
        }
    )
}