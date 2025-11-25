package org.zotero.android.pdf.reader.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.pdf.data.PdfReaderTool
import org.zotero.android.pdf.reader.DragAnchors
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
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
internal fun BoxScope.PdfReaderAnnotationCreationToolbar(
    viewState: PdfReaderViewState,
    vMInterface: PdfReaderVMInterface,
    state: AnchoredDraggableState<DragAnchors>,
    onShowSnapTargetAreas: () -> Unit,
    shouldShowSnapTargetAreas: Boolean,
) {
    val roundCornerShape = RoundedCornerShape(size = 12.dp)
    val draggableInteractionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(key1 = vMInterface) {
        draggableInteractionSource.interactions.onEach {
            onShowSnapTargetAreas()
        }.launchIn(coroutineScope)
    }
    val snapAreaBackgroundColor =
        MaterialTheme.colorScheme.surface
    if (shouldShowSnapTargetAreas) {
        val stroke = Stroke(
            width = 5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        val strokeColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(520.dp)
                .padding(start = 16.dp, top = 16.dp)
                .background(
                    color = snapAreaBackgroundColor,
                    roundCornerShape
                )
                .clip(roundCornerShape)
                .drawBehind {
                    drawRoundRect(color = strokeColor, style = stroke)
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(48.dp)
                .height(520.dp)
                .padding(end = 16.dp, top = 16.dp)
                .background(
                    color = snapAreaBackgroundColor,
                    roundCornerShape
                )
                .clip(roundCornerShape)
                .drawBehind {
                    drawRoundRect(color = strokeColor, style = stroke)
                }
        )
    }

    LazyColumn(
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
            .height(520.dp)
            .padding(start = 16.dp, top = 16.dp)
            .background(
                color = snapAreaBackgroundColor,
                shape = roundCornerShape
            )
            .clip(roundCornerShape)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            pdfReaderToolsList.forEach { tool ->
                if (!tool.isHidden) {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above,
                            4.dp
                        ),
                        tooltip = {
                            PlainTooltip() {
                                Text(
                                    text = stringResource(tool.title)
                                )
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        PdfReaderAnnotationCreationToggleButton(
                            activeAnnotationTool = vMInterface.activeAnnotationTool,
                            pdfReaderTool = tool,
                            toggleButton = vMInterface::toggle
                        )

                    }

                }
            }
            val activeAnnotationTool = vMInterface.activeAnnotationTool
            if (viewState.isColorPickerButtonVisible && activeAnnotationTool != null) {
                if (activeAnnotationTool == AnnotationTool.ERASER) {
                    PdfReaderEmptyFilterCircle(onClick = vMInterface::showToolOptions)
                } else {
                    val color = vMInterface.toolColors[activeAnnotationTool]
                    if (color != null) {
                        PdfReaderFilledFilterCircle(hex = color, onClick = vMInterface::showToolOptions)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.accessibility_pdf_undo
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                PdfReaderAnnotationCreationButton(
                    isEnabled = vMInterface.canUndo(),
                    iconInt = Drawables.undo_24px,
                    onButtonClick = vMInterface::onUndoClick
                )
            }
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.accessibility_pdf_redo
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                PdfReaderAnnotationCreationButton(
                    isEnabled = vMInterface.canRedo(),
                    iconInt = Drawables.redo_24px,
                    onButtonClick = vMInterface::onRedoClick
                )
            }

            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.cancel
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                PdfReaderAnnotationCreationButton(
                    isEnabled = true,
                    iconInt = Drawables.cancel_24px,
                    onButtonClick = vMInterface::onCloseClick
                )

            }

            PdfReaderAnnotationCreationButton(
                isEnabled = true,
                iconInt = Drawables.drag_handle,
            )
        }
    }
}
