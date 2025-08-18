package org.zotero.android.screens.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun LoginTopBar(
    onCancelClicked: () -> Unit,
) {
    NewCustomTopBar(
        shouldAddBottomDivider = false,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onCancelClicked,
                text = stringResource(id = Strings.cancel),
            )
        }
    )
}
