package org.zotero.android.screens.share

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ShareScreenTopBar(
    onCancelClicked: () -> Unit,
    onSave: () -> Unit,
    isLeftButtonEnabled: Boolean,
    isRightButtonEnabled: Boolean,
    attachmentError: AttachmentState.Error?,
    ) {
    when (attachmentError) {
        is AttachmentState.Error.quotaLimit, is AttachmentState.Error.webDavFailure, is AttachmentState.Error.apiFailure -> {
            NewCustomTopBar(
                shouldAddBottomDivider = false,
                rightContainerContent = listOf {
                    NewHeadingTextButton(
                        style = CustomTheme.typography.defaultBold,
                        text = stringResource(id = Strings.done),
                        onClick = onCancelClicked
                    )
                }
            )
        }
        else -> {
            NewCustomTopBar(
                shouldAddBottomDivider = false,
                leftContainerContent = listOf {
                    NewHeadingTextButton(
                        text = stringResource(id = Strings.cancel),
                        isEnabled = isLeftButtonEnabled,
                        onClick = onCancelClicked
                    )
                },
                rightContainerContent = listOf {
                    NewHeadingTextButton(
                        style = CustomTheme.typography.defaultBold,
                        text = stringResource(id = Strings.shareext_save),
                        isEnabled = isRightButtonEnabled,
                        onClick = onSave
                    )
                }
            )
        }
    }

}