package org.zotero.android.pdf.annotation.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PdfAnnotationColorPicker(
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colors.forEach { listColorHex ->
            PdfAnnotationFilterCircle(
                hex = listColorHex,
                isSelected = listColorHex == selectedColor,
                onClick = { onColorSelected(listColorHex) })
        }
    }
}
