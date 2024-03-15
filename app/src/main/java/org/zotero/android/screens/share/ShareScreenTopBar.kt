package org.zotero.android.screens.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ShareScreenTopBar(
    onCancelClicked: () -> Unit,
    onSave: () -> Unit,
) {
    NewCustomTopBar(
        shouldAddBottomDivider = false,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.cancel),
                onClick = onCancelClicked
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                style = CustomTheme.typography.defaultBold,
                text = stringResource(id = Strings.shareext_save),
                onClick = onSave
            )
        }
    )
}