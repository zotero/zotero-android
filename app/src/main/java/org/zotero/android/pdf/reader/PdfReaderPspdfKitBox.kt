package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import org.zotero.android.pdf.data.PdfReaderTool
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

private val pdfReaderToolsList = listOf(
    PdfReaderTool(
        type = AnnotationTool.HIGHLIGHT,
        title = Strings.pdf_annotation_toolbar_highlight,
        image = Drawables.highlighter_large,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.NOTE,
        title = Strings.pdf_annotation_toolbar_note,
        image = Drawables.note_large,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.SQUARE,
        title = Strings.pdf_annotation_toolbar_image,
        image = Drawables.area_large,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.INK,
        title = Strings.pdf_annotation_toolbar_ink,
        image = Drawables.ink_large,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.ERASER,
        title = Strings.pdf_annotation_toolbar_eraser,
        image = Drawables.eraser_large,
        isHidden = false
    )
)

@Composable
internal fun PdfReaderPspdfKitBox(
    uri: Uri,
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PdfReaderPspdfKitView(uri = uri, viewModel = viewModel)
        if (viewState.showCreationToolbar) {
            PdfReaderAnnotationCreationToolbar(
                viewModel = viewModel,
                viewState = viewState
            )
        }
    }
}

@Composable
fun PdfReaderAnnotationCreationToolbar(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState
) {
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    Box(
        modifier = Modifier
            .height(500.dp)
            .padding(start = 20.dp, top = 20.dp)
            .background(
                color = CustomTheme.colors.pdfToolbarBackgroundColor,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)
    ) {
        Column(modifier = Modifier)
        {
            Spacer(modifier = Modifier.height(20.dp))
            pdfReaderToolsList.forEach { tool ->
                if (!tool.isHidden) {
                    AnnotationCreationToggleButton(
                        viewModel = viewModel,
                        pdfReaderTool = tool,
                        toggleButton = viewModel::toggle
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            val activeAnnotationTool = viewModel.activeAnnotationTool
            if (viewState.isColorPickerButtonVisible && activeAnnotationTool != null) {
                if (activeAnnotationTool == AnnotationTool.ERASER) {
                    EmptyFilterCircle(onClick = { viewModel.showToolOptions() })
                } else {
                    val color = viewModel.toolColors[activeAnnotationTool]
                    if (color != null) {
                        FilledFilterCircle(hex = color, onClick = { viewModel.showToolOptions() })
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnnotationCreationButton(
                isEnabled = viewModel.canUndo(),
                iconInt = Drawables.baseline_undo_24,
                onButtonClick = viewModel::onUndoClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            AnnotationCreationButton(
                isEnabled = viewModel.canRedo(),
                iconInt = Drawables.baseline_redo_24,
                onButtonClick = viewModel::onRedoClick
            )
            Spacer(modifier = Modifier.height(20.dp))
            AnnotationCreationButton(
                isEnabled = true,
                iconInt = Drawables.x_mark_circle,
                onButtonClick = viewModel::onCloseClick
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AnnotationCreationToggleButton(
    viewModel: PdfReaderViewModel,
    pdfReaderTool: PdfReaderTool,
    toggleButton: (AnnotationTool) -> Unit
) {
    val isSelected = viewModel.activeAnnotationTool == pdfReaderTool.type
    val tintColor = if (isSelected) {
        Color.White
    } else {
        CustomTheme.colors.zoteroBlueWithDarkMode
    }
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    var modifier = Modifier
        .padding(horizontal = 8.dp)
        .size(28.dp)
        .clip(roundCornerShape)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { toggleButton(pdfReaderTool.type) },
        )
    if (isSelected) {
        modifier = modifier.background(
            color = CustomTheme.colors.zoteroBlueWithDarkMode,
            shape = roundCornerShape
        )
    }
    Icon(
        modifier = modifier,
        painter = painterResource(id = pdfReaderTool.image),
        contentDescription = null,
        tint = tintColor
    )
}

@Composable
private fun FilledFilterCircle(hex: String, onClick: () -> Unit) {
    val color = android.graphics.Color.parseColor(hex)
    Canvas(modifier = Modifier
        .padding(horizontal = 8.dp)
        .size(28.dp)
        .safeClickable(
            onClick = onClick, interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ), onDraw = {
        drawCircle(color = Color(color))
    })
}

@Composable
private fun EmptyFilterCircle(onClick: () -> Unit) {
    val color = CustomTheme.colors.zoteroBlueWithDarkMode
    Canvas(modifier = Modifier
        .padding(horizontal = 8.dp)
        .size(28.dp)
        .safeClickable(
            onClick = onClick, interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ), onDraw = {
        drawCircle(color = color, style = Stroke(1.5.dp.toPx()))
    })
}

@Composable
private fun AnnotationCreationButton(
    isEnabled: Boolean,
    iconInt: Int,
    onButtonClick: () -> Unit
) {
    val tintColor = if (isEnabled) {
        CustomTheme.colors.zoteroBlueWithDarkMode
    } else {
        CustomTheme.colors.zoteroBlueWithDarkMode.copy(alpha = 0.5f)
    }
    val modifier = Modifier
        .padding(horizontal = 8.dp)
        .size(28.dp)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onButtonClick,
            enabled = isEnabled
        )
    Icon(
        modifier = modifier,
        painter = painterResource(id = iconInt),
        contentDescription = null,
        tint = tintColor
    )
}

