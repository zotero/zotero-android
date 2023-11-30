package org.zotero.android.screens.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.screens.filter.data.FilterDialog
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.theme.CustomPalette

@Composable
internal fun FilterDeleteAutomaticTagsDialog(
    filterDialog: FilterDialog,
    onDismissDialog: () -> Unit,
    onDeleteAutomaticTags: () -> Unit,
) {
    when (filterDialog) {
        is FilterDialog.confirmDeletion -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.tag_picker_confirm_deletion_question),
                description = quantityStringResource(
                    id = Plurals.tag_picker_confirm_deletion,
                    filterDialog.count
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.cancel),
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onDeleteAutomaticTags()
                    }
                ),
                onDismiss = onDismissDialog
            )
        }

        else -> {}
    }
}
