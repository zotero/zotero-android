package org.zotero.android.screens.itemdetails.rows

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.zotero.android.screens.itemdetails.ItemDetailsViewModel
import org.zotero.android.screens.itemdetails.ItemDetailsViewState
import org.zotero.android.screens.itemdetails.data.ItemDetailField

@Composable
fun ItemDetailsListOfFieldRows(
    viewState: ItemDetailsViewState,
    viewModel: ItemDetailsViewModel,
) {
    for (fieldId in viewState.data.fieldIds) {
        val field = viewState.data.fields[fieldId] ?: continue
        val title = field.name
        var value = field.additionalInfo?.get(ItemDetailField.AdditionalInfoKey.formattedDate)
            ?: field.value
        value = value.ifEmpty { " " }
        val textColor = if (field.isTappable) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        ItemDetailsFieldRow(
            detailTitle = title,
            detailValue = value,
            textColor = textColor,
            additionalInfoString = field.additionalInfo?.get(ItemDetailField.AdditionalInfoKey.dateOrder),
            onRowTapped = { viewModel.onRowTapped(field) }
        )
    }
}
