package org.zotero.android.screens.citbibexport

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun CitBibExportItemWithDescription(
    title: String,
    description: String,
    isEnabled: Boolean = true,
    onItemTapped: () -> Unit,
    onItemLongTapped: (() -> Unit)? = null,
) {
    val disabledColor = Color(0xFF89898C)
    val titleTextColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        disabledColor
    }

    val descriptionTextColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        disabledColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = isEnabled,
                onClick = { onItemTapped() },
                onLongClick = onItemLongTapped
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleTextColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = descriptionTextColor,
            )
        }
    }
}