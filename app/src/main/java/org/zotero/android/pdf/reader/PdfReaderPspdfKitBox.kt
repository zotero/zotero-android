package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.pdf.data.PdfReaderTool
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.roundToInt

private val pdfReaderToolsList = listOf(
    PdfReaderTool(
        type = AnnotationTool.HIGHLIGHT,
        title = Strings.pdf_annotation_toolbar_highlight,
        image = Drawables.annotate_highlight,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.UNDERLINE,
        title = Strings.pdf_annotation_toolbar_underline,
        image = Drawables.annotate_underline,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.NOTE,
        title = Strings.pdf_annotation_toolbar_note,
        image = Drawables.annotate_note,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.FREETEXT,
        title = Strings.pdf_annotation_toolbar_text,
        image = Drawables.annotate_text,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.SQUARE,
        title = Strings.pdf_annotation_toolbar_image,
        image = Drawables.annotate_area,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.INK,
        title = Strings.pdf_annotation_toolbar_ink,
        image = Drawables.annotate_ink,
        isHidden = false
    ),
    PdfReaderTool(
        type = AnnotationTool.ERASER,
        title = Strings.pdf_annotation_toolbar_eraser,
        image = Drawables.annotate_eraser,
        isHidden = false
    )
)

@Composable
internal fun PdfReaderPspdfKitBox(
    uri: Uri,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState
) {
    val density = LocalDensity.current
    val positionalThreshold = { distance: Float -> distance * 0.5f }
    val velocityThreshold = { with(density) { 1000.dp.toPx() } }
    val animationSpec = tween<Float>()

    var shouldShowSnapTargetAreas: Boolean by remember { mutableStateOf(false) }
    val confirmValueChange = { newValue: DragAnchors ->
        shouldShowSnapTargetAreas = false
        true
    }
    val anchoredDraggableState = rememberSaveable(
        saver = AnchoredDraggableState.Saver(
            animationSpec = animationSpec,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            confirmValueChange = confirmValueChange,
        )
    ) {
        AnchoredDraggableState(
            initialValue = DragAnchors.Start,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
        )
    }
    val rightTargetAreaXOffset = with(density) { 92.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { layoutSize ->
                val dragEndPoint = layoutSize.width - rightTargetAreaXOffset
                anchoredDraggableState.updateAnchors(
                    DraggableAnchors {
                        DragAnchors.entries
                            .forEach { anchor ->
                                anchor at dragEndPoint * anchor.fraction
                            }
                    }
                )
            }
    ) {
        PdfReaderPspdfKitView(uri = uri, vMInterface = vMInterface)
        if (viewState.showCreationToolbar) {
            PdfReaderAnnotationCreationToolbar(
                viewState = viewState,
                vMInterface = vMInterface,
                state = anchoredDraggableState,
                onShowSnapTargetAreas = { shouldShowSnapTargetAreas = true },
                shouldShowSnapTargetAreas = shouldShowSnapTargetAreas
            )
        }
    }
}

enum class DragAnchors(val fraction: Float) {
    Start(0f),
    End(1f),
}

@Composable
fun BoxScope.PdfReaderAnnotationCreationToolbar(
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    state: AnchoredDraggableState<DragAnchors>,
    onShowSnapTargetAreas: () -> Unit,
    shouldShowSnapTargetAreas: Boolean,
) {
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    val draggableInteractionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = vMInterface) {
        draggableInteractionSource.interactions.onEach {
            onShowSnapTargetAreas()
        }.launchIn(coroutineScope)
    }

    if (shouldShowSnapTargetAreas) {
        val stroke = Stroke(
            width = 5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        val snapAreaBackgroundColor =
            if (isSystemInDarkTheme()) Color(0xFF080E1C) else Color(0xFFE2EAFB)
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 500.dp)
                .padding(start = 20.dp, top = 20.dp)
                .background(
                    color = snapAreaBackgroundColor,
                    roundCornerShape
                )
                .clip(roundCornerShape)
                .drawBehind {
                    drawRoundRect(color = Color(0xFF4978E7), style = stroke)
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 72.dp, height = 500.dp)
                .padding(end = 20.dp, top = 20.dp)
                .background(
                    color = snapAreaBackgroundColor,
                    roundCornerShape
                )
                .clip(roundCornerShape)
                .drawBehind {
                    drawRoundRect(color = Color(0xFF4978E7), style = stroke)
                }
        )
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = state
                        .requireOffset()
                        .roundToInt(),
                    y = 0,
                )
            }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                interactionSource = draggableInteractionSource
            )
            .height(580.dp)
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
                        activeAnnotationTool = vMInterface.activeAnnotationTool,
                        pdfReaderTool = tool,
                        toggleButton = vMInterface::toggle
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
            val activeAnnotationTool = vMInterface.activeAnnotationTool
            if (viewState.isColorPickerButtonVisible && activeAnnotationTool != null) {
                if (activeAnnotationTool == AnnotationTool.ERASER) {
                    EmptyFilterCircle(onClick = vMInterface::showToolOptions)
                } else {
                    val color = vMInterface.toolColors[activeAnnotationTool]
                    if (color != null) {
                        FilledFilterCircle(hex = color, onClick = vMInterface::showToolOptions)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AnnotationCreationButton(
                isEnabled = vMInterface.canUndo(),
                iconInt = Drawables.undo_24px,
                onButtonClick = vMInterface::onUndoClick
            )
            Spacer(modifier = Modifier.height(15.dp))
            AnnotationCreationButton(
                isEnabled = vMInterface.canRedo(),
                iconInt = Drawables.redo_24px,
                onButtonClick = vMInterface::onRedoClick
            )
            Spacer(modifier = Modifier.height(15.dp))
            AnnotationCreationButton(
                isEnabled = true,
                iconInt = Drawables.cancel_24px,
                onButtonClick = vMInterface::onCloseClick
            )
            Spacer(modifier = Modifier.height(15.dp))
        }
    }
}

@Composable
private fun AnnotationCreationToggleButton(
    activeAnnotationTool: AnnotationTool?,
    pdfReaderTool: PdfReaderTool,
    toggleButton: (AnnotationTool) -> Unit
) {
    val isSelected = activeAnnotationTool == pdfReaderTool.type
    val tintColor = if (isSelected) {
        Color.White
    } else {
        CustomTheme.colors.zoteroDefaultBlue
    }
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    var modifier = Modifier
        .padding(horizontal = 10.dp)
        .size(32.dp)
        .clip(roundCornerShape)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { toggleButton(pdfReaderTool.type) },
        )
    if (isSelected) {
        modifier = modifier.background(
            color = CustomTheme.colors.zoteroDefaultBlue,
            shape = roundCornerShape
        )
    }
    Icon(
        modifier = modifier.padding(2.dp),
        painter = painterResource(id = pdfReaderTool.image),
        contentDescription = null,
        tint = tintColor
    )
}

@Composable
private fun FilledFilterCircle(hex: String, onClick: () -> Unit) {
    val color = android.graphics.Color.parseColor(hex)
    Canvas(modifier = Modifier
        .padding(horizontal = 10.dp)
        .size(30.dp)
        .padding(2.dp)
        .safeClickable(
            onClick = onClick, interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ), onDraw = {
        drawCircle(color = Color(color))
    })
}

@Composable
private fun EmptyFilterCircle(onClick: () -> Unit) {
    val color = CustomTheme.colors.zoteroDefaultBlue
    Canvas(modifier = Modifier
        .padding(horizontal = 10.dp)
        .size(30.dp)
        .padding(2.dp)
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
        CustomTheme.colors.zoteroDefaultBlue
    } else {
        CustomTheme.colors.zoteroDefaultBlue.copy(alpha = 0.5f)
    }
    val modifier = Modifier
        .padding(horizontal = 10.dp)
        .size(32.dp)
        .padding(2.dp)
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

