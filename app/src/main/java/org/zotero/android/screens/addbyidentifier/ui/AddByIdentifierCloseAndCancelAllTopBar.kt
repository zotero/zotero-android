package org.zotero.android.screens.addbyidentifier.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun AddByIdentifierCloseAndCancelAllTopBar(
    onClose: () -> Unit,
    onCancelAll: () -> Unit,
) {
    NewCustomTopBar(
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.close),
                onClick = onClose
            )
            NewHeadingTextButton(
                text = stringResource(id = Strings.add_by_identifier_cancel_all_button),
                onClick = onCancelAll
            )
        },
    )
}