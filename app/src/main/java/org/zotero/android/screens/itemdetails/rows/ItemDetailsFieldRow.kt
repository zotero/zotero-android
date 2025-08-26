package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun ItemDetailsFieldRow(
    detailTitle: String,
    detailValue: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    additionalInfoString: String? = null,
    onRowTapped: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .safeClickable(
                onClick = onRowTapped,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ), verticalAlignment = Alignment.CenterVertically
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        Column(modifier = Modifier.width(layoutType.calculateItemFieldLabelWidth())) {
            Text(
                modifier = Modifier.align(Alignment.End),
                text = detailTitle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                modifier = Modifier,
                text = detailValue,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (additionalInfoString != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = additionalInfoString,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}


