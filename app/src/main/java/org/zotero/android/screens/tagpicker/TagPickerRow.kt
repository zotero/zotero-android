package org.zotero.android.screens.tagpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun TagPickerRow(
    text: String,
    rowBackgroundColor: Color,
    tagColorHex: String?,
    isChecked: Boolean,
    onTap: () -> Unit
) {
    val selectableBackgroundColor =
        if (isChecked) Color(0xFFF2F2F7) else rowBackgroundColor

    Box(
        modifier = Modifier
            .height(44.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onTap
            )
            .background(color = selectableBackgroundColor)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            TagPickerCheckBox(
                isChecked = isChecked,
            )

            if (!tagColorHex.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(16.dp))
                TagPickerCircle(tagColorHex)
            }

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = CustomTheme.typography.newBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = CustomTheme.colors.primaryContent,
            )

            Spacer(modifier = Modifier.width(16.dp))
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp)
        )
    }
}

@Composable
private fun TagPickerCircle(tagColorHex: String) {
    val colorInt = android.graphics.Color.parseColor(tagColorHex)
    Canvas(modifier = Modifier.size(16.dp),
        onDraw = {
            drawCircle(color = Color(colorInt))
        }
    )
}
