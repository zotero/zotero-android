package org.zotero.android.screens.collectionedit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun CollectionEditTopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewState: CollectionEditViewState
) {
    NewCustomTopBar(
        title = stringResource(
            id = if (viewState.key != null) {
                Strings.collections_edit_title
            } else {
                Strings.collections_create_title
            }
        ),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onCancel,
                text = stringResource(Strings.cancel),
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onSave,
                isEnabled = viewState.isValid,
                text = stringResource(Strings.save),
            )
        }
    )
}
