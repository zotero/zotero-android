package org.zotero.android.pdf.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ColorPicker(
    viewState: PdfAnnotationViewState,
    viewModel: PdfAnnotationViewModel
) {
    val selectedColor = viewState.color
    FlowRow(
        modifier = Modifier.padding(horizontal = 10.dp),
    ) {
        viewState.colors.forEach { listColorHex ->
            FilterCircle(
                hex = listColorHex,
                isSelected = listColorHex == selectedColor,
                onClick = { viewModel.onColorSelected(listColorHex) })
        }
    }
}

@Composable
internal fun FilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = android.graphics.Color.parseColor(hex)
    Canvas(modifier = Modifier
        .padding(4.dp)
        .size(32.dp)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ), onDraw = {
        drawCircle(color = Color(color))
        if (isSelected) {
            drawCircle(
                color = CustomPalette.White,
                radius = 12.dp.toPx(),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    })
}

@Composable
internal fun SizeSelector(
    viewState: PdfAnnotationViewState,
    viewModel: PdfAnnotationViewModel,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp),
            text = stringResource(id = Strings.size),
            color = CustomTheme.colors.pdfSizePickerColor,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
        Slider(
            modifier = Modifier.weight(1f),
            value = viewState.size,
            onValueChange = { viewModel.onSizeChanged(it) },
            colors = SliderDefaults.colors(
                activeTrackColor = CustomTheme.colors.zoteroDefaultBlue,
                thumbColor = CustomTheme.colors.zoteroDefaultBlue,
            ),
            valueRange = 0.5f..25f
        )
        Text(
            modifier = Modifier.padding(horizontal = 10.dp),
            text = String.format("%.1f", viewState.size),
            color = CustomTheme.colors.pdfSizePickerColor,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    }
}

@Composable
internal fun FontSizeSelector(
    fontSize: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%.1f", fontSize),
                color = CustomTheme.colors.defaultTextColor,
                style = CustomTheme.typography.default,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "pt",
                color = CustomTheme.colors.pdfSizePickerColor,
                style = CustomTheme.typography.default,
                fontSize = 16.sp,
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            FontSizeChangeButton(text = "-", onClick = onFontSizeDecrease)
            Spacer(modifier = Modifier.width(2.dp))
            FontSizeChangeButton(text = "+", onClick = onFontSizeIncrease)
        }
    }
}

@Composable
private fun FontSizeChangeButton(text: String, onClick: (() -> Unit)) {
    val roundCornerShape = RoundedCornerShape(size = 8.dp)

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .background(
                color = CustomTheme.colors.pdfAnnotationsFormBackground,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick,
            )
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = text,
            color = CustomTheme.colors.defaultTextColor,
            style = CustomTheme.typography.default,
            fontSize = 22.sp,
        )
    }
}