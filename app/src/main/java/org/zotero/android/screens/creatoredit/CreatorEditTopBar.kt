package org.zotero.android.screens.creatoredit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun CreatorEditTopBar(
    onCloseClicked: () -> Unit,
    onSave: () -> Unit,
    viewState: CreatorEditViewState
) {
    NewCustomTopBar(
        backgroundColor = CustomTheme.colors.surface,
        title = viewState.creator?.localizedType,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onCloseClicked,
                text = stringResource(Strings.cancel),
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onSave,
                text = stringResource(Strings.save),
                isEnabled = viewState.isValid
            )
        }
    )
}
