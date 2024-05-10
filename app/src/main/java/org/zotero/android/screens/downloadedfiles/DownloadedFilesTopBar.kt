package org.zotero.android.screens.downloadedfiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun DownloadedFilesTopBar(
    onDone: () -> Unit,
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.items_filters_title),
        rightContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.done),
                onClick = onDone
            )
        }
    )
}