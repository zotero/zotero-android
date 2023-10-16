package org.zotero.android.pdf.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CommentSection(
    viewState: PdfAnnotationViewState,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: PdfAnnotationViewModel
) {
    CustomTextField(
        modifier = Modifier
            .padding(start = 8.dp),
        value = viewState.commentFocusText,
        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculatePdfSidebarTextSize()),
        hint = stringResource(id = Strings.pdf_annotations_sidebar_add_comment),
        ignoreTabsAndCaretReturns = false,
        minLines = 5,
        onValueChange = { viewModel.onCommentTextChange(it) })
}

@Composable
internal fun TagsSection(
    viewModel: PdfAnnotationViewModel,
    viewState: PdfAnnotationViewState,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (!viewState.tags.isEmpty()) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.onTagsClicked() }
                ),
            text = viewState.tags.joinToString(separator = ", ") { it.name },
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    } else {
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.onTagsClicked() }
                ),
            text = stringResource(id = Strings.pdf_annotations_sidebar_add_tags),
            color = CustomTheme.colors.zoteroBlueWithDarkMode,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    }
}

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
private fun FilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
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


