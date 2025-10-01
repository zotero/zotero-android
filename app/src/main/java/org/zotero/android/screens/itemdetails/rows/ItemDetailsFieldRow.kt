package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.reorder.ReorderableState
import org.zotero.android.uicomponents.reorder.detectReorderAfterLongPress
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ItemDetailsFieldRow(
    detailTitle: String,
    detailValue: String,
    layoutType: CustomLayoutSize.LayoutType,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    reorderState: ReorderableState? = null,
    additionalInfoString: String? = null,
    onDelete: (() -> Unit)? = null,
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
        ItemDetailsFieldRowOnDelete(onDelete)
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
                style = CustomTheme.typography.newBody,
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
        if (reorderState != null) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier
                    .size(28.dp)
                    .detectReorderAfterLongPress(reorderState),
                painter = painterResource(id = Drawables.drag_handle_24px),
                contentDescription = null,
                colorFilter = ColorFilter.tint(CustomTheme.colors.reorderButtonColor),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}


