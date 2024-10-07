package org.zotero.android.screens.addbyidentifier.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun AddByIdentifierTopBar(
    title: String?,
    onCancel: () -> Unit,
    onLookup: () -> Unit,
) {
    NewCustomTopBar(
        title = title,
        leftContainerContent = listOf {
            NewHeadingTextButton(text = stringResource(id = Strings.cancel), onClick = onCancel)
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                style = CustomTheme.typography.defaultBold,
                text = stringResource(id = Strings.look_up),
                onClick = onLookup
            )
        }
    )
}
