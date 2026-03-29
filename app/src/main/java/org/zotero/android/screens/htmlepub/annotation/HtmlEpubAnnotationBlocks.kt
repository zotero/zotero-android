package org.zotero.android.screens.htmlepub.annotation


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.zotero.android.screens.htmlepub.annotationmore.HtmlEpubAnnotationFilterCircle
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import java.util.Locale

@Composable
internal fun HtmlEpubAnnotationColorPicker(
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
            HtmlEpubAnnotationFilterCircle(
                hex = listColorHex,
                isSelected = listColorHex == selectedColor,
                onClick = { onColorSelected(listColorHex) })
        }
    }
}


@Composable
internal fun HtmlEpubAnnotationFilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = hex.toColorInt()
    Canvas(modifier = Modifier
        .size(28.dp)
        .debounceClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ), onDraw = {
        drawCircle(color = Color(color))
        if (isSelected) {
            drawCircle(
                color = CustomPalette.White,
                radius = 11.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    })
}


@Composable
internal fun HtmlEpubAnnotationFontSizeChangeButton(text: String, onClick: (() -> Unit)) {
    val roundCornerShape = RoundedCornerShape(size = 8.dp)

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 22.sp,
        )
    }
}


@Composable
internal fun HtmlEpubAnnotationFontSizeSelector(
    fontSize: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", fontSize),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "pt",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            HtmlEpubAnnotationFontSizeChangeButton(text = "-", onClick = onFontSizeDecrease)
            Spacer(modifier = Modifier.width(2.dp))
            HtmlEpubAnnotationFontSizeChangeButton(text = "+", onClick = onFontSizeIncrease)
        }
    }
}


@Composable
internal fun HtmlEpubAnnotationSizeSelector(
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
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = size,
            onValueChange = onSizeChanged,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary,
            ),
            valueRange = 0.5f..25f
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = String.format(Locale.getDefault(), "%.1f", size),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
