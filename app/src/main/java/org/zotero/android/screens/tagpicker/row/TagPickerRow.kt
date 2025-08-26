package org.zotero.android.screens.tagpicker.row

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.screens.tagpicker.TagPickerCheckBox
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
fun TagPickerRow(
    text: String,
    tagColorHex: String?,
    isChecked: Boolean,
    onTap: () -> Unit
) {
    var rowModifier: Modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
    val isRowSelected = isChecked
    if (isRowSelected) {
        val roundCornerShape = RoundedCornerShape(8.dp)
        rowModifier = rowModifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)

    }
    Row(
        modifier = rowModifier
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onTap
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRowSelected) {
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (!tagColorHex.isNullOrEmpty()) {
            TagPickerCircle(tagColorHex)
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        TagPickerCheckBox(
            isChecked = isChecked,
            onCheckedChange = { onTap() }
        )

        if (!isRowSelected) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun TagPickerCircle(tagColorHex: String) {
    val colorInt = tagColorHex.toColorInt()
    Canvas(
        modifier = Modifier.size(16.dp),
        onDraw = {
            drawCircle(color = Color(colorInt))
        }
    )
}
