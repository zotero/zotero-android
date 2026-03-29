package org.zotero.android.screens.htmlepub.annotationmore


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import java.util.Locale

@Composable
internal fun HtmlEpubAnnotationMoreColorPicker(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val selectedColor = viewState.color
        viewState.colors.forEach { listColorHex ->
            HtmlEpubAnnotationMoreFilterCircle(
                hex = listColorHex,
                isSelected = listColorHex == selectedColor,
                onClick = { viewModel.onColorSelected(listColorHex) })
        }
    }

}


@Composable
internal fun HtmlEpubAnnotationMoreFilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
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
internal fun HtmlEpubAnnotationMoreFontSizeChangeButton(text: String, onClick: (() -> Unit)) {
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
internal fun HtmlEpubAnnotationMoreFontSizeSelector(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", viewState.fontSize),
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
            HtmlEpubAnnotationMoreFontSizeChangeButton(text = "-", onClick = viewModel::onFontSizeDecrease)
            Spacer(modifier = Modifier.width(2.dp))
            HtmlEpubAnnotationMoreFontSizeChangeButton(text = "+", onClick = viewModel::onFontSizeIncrease)
        }
    }
}


@Composable
internal fun HtmlEpubAnnotationMoreHighlightText(
    annotationColor: Color,
    viewState: HtmlEpubAnnotationMoreViewState,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(annotationColor)
        )

        CustomTextField(
            modifier = Modifier.padding(start = 27.dp, end = 16.dp),
            value = viewState.highlightText,
            hint = "",
            ignoreTabsAndCaretReturns = false,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


@Composable
internal fun HtmlEpubAnnotationMorePageButton(
    viewModel: HtmlEpubAnnotationMoreViewModel,
    viewState: HtmlEpubAnnotationMoreViewState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                onClick = viewModel::onPageClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(Strings.page_number),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = viewState.pageLabel,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

}

@Composable
internal fun HtmlEpubAnnotationMoreUnderlineText(
    annotationColor: Color,
    viewState: HtmlEpubAnnotationMoreViewState,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(3.dp)
                .fillMaxHeight()
                .background(annotationColor)
        )

        CustomTextField(
            modifier = Modifier.padding(start = 27.dp, end = 16.dp),
            value = viewState.underlineText,
            hint = "",
            ignoreTabsAndCaretReturns = false,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


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

