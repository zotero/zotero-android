package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.screens.itemdetails.ItemDetailsViewState

@Composable
internal fun ItemDetailsEditListOfFieldRows(
    viewState: ItemDetailsViewState,
    onValueChange: (String, String) -> Unit,
    onFocusChanges: (String) -> Unit,
) {
    for (fieldId in viewState.data.fieldIds) {
        val field = viewState.data.fields[fieldId] ?: continue
        val title = field.name
        val value = if (field.key == viewState.fieldFocusKey) {
            viewState.fieldFocusText
        } else {
            field.valueOrAdditionalInfo
        }

        if (viewState.data.isAttachment) {
            ItemDetailsEditFieldReadOnlyRow(title = title, value = value)
        } else {
            ItemDetailsEditFieldEditableRow(
                key = field.key,
                fieldId = fieldId,
                detailTitle = title,
                detailValue = value,
                textColor = MaterialTheme.colorScheme.onSurface,
                onValueChange = onValueChange,
                isMultilineAllowed = field.key == FieldKeys.Item.extra,
                onFocusChanges = onFocusChanges
            )
        }

    }
}
