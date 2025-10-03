package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.rows.ItemDetailsFieldRow
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun ItemDetailsEditFieldReadOnlyRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    //no action on tap, but still show ripple effect
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemDetailsFieldRow(
            detailTitle = title,
            detailValue = value,
        )
    }
}
