package org.zotero.android.pdf.annotation.blocks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import java.util.Locale

@Composable
internal fun PdfAnnotationSizeSelector(
    size: Float,
    onSizeChanged: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 10.dp),
            text = stringResource(id = Strings.size),
            color = CustomTheme.colors.pdfSizePickerColor,
            style = CustomTheme.typography.default,
            fontSize = 16.sp,
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = size,
            onValueChange = onSizeChanged,
            colors = SliderDefaults.colors(
                activeTrackColor = CustomTheme.colors.zoteroDefaultBlue,
                thumbColor = CustomTheme.colors.zoteroDefaultBlue,
            ),
            valueRange = 0.5f..25f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = String.format(Locale.getDefault(), "%.1f", size),
            color = CustomTheme.colors.pdfSizePickerColor,
            style = CustomTheme.typography.default,
            fontSize = 16.sp,
        )
    }
}
