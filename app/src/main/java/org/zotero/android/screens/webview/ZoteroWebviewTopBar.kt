package org.zotero.android.screens.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ZoteroWebviewTopBar(
    onClose: () -> Unit,
) {
    NewCustomTopBar(
        backgroundColor = CustomTheme.colors.surface,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.close),
                onClick = onClose
            )
        },
    )
}
