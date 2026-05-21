package org.zotero.android.screens.filter

import androidx.compose.runtime.Composable
import org.zotero.android.screens.filter.data.FilterDialog
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeQuantityStringResource
import org.zotero.android.uicomponents.foundation.safeStringResource
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3
import org.zotero.android.uicomponents.modal.CustomAlertDialogM3ActionConfig
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun FilterDeleteAutomaticTagsDialog(
    filterDialog: FilterDialog,
    onDismissDialog: () -> Unit,
    onDeleteAutomaticTags: () -> Unit,
) {
    when (filterDialog) {
        is FilterDialog.confirmDeletion -> {
            CustomAlertDialogM3(
                title = safeStringResource(id = Strings.tag_picker_confirm_deletion_question),
                description = safeQuantityStringResource(
                    id = Plurals.tag_picker_confirm_deletion,
                    filterDialog.count
                ),
                dismissButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.cancel),
                ),
                confirmButton = CustomAlertDialogM3ActionConfig(
                    text = safeStringResource(id = Strings.ok),
                    textColor = CustomPalette.ErrorRed,
                    onClick = onDeleteAutomaticTags,
                ),
                onDismiss = onDismissDialog
            )
        }
    }
}
