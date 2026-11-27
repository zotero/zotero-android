package org.zotero.android.screens.reader.toolbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.zotero.android.screens.reader.ReaderToolbarPosition
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.toolbar.data.ReaderTool
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.foundation.safeStringResource
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val TOOLBAR_CHROME_HEIGHT = 268.dp
private val TOOLBAR_TOOL_HEIGHT = 40.dp
private val TOOLBAR_ITEM_SIZE = 48.dp
private val TOOLBAR_MARGIN = 16.dp
private val TOOLBAR_PINNED_CONTENT_HEIGHT = 64.dp
private val PLACEHOLDER_DASH_WIDTH = 3.dp
private val PLACEHOLDER_DASH_ON = 6.dp
private val PLACEHOLDER_DASH_OFF = 3.dp
private val PLACEHOLDER_CORNER_RADIUS = 8.dp
private val PLACEHOLDER_SHAPE = RoundedCornerShape(PLACEHOLDER_CORNER_RADIUS)
private val TOOLBAR_OVERLAY_SIDEBAR_INSET = 330.dp
private val TOOLBAR_HORIZONTAL_SPACER = 8.dp

private val htmlEpubReaderToolsList = listOf(
    ReaderTool(
        type = ReaderAnnotationTool.highlight,
        title = Strings.pdf_annotation_toolbar_highlight,
        image = Drawables.annotate_highlight,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.underline,
        title = Strings.pdf_annotation_toolbar_underline,
        image = Drawables.annotate_underline,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.note,
        title = Strings.pdf_annotation_toolbar_note,
        image = Drawables.annotate_note,
        isHidden = false
    ),
)

private val pdfReaderToolsList = listOf(
    ReaderTool(
        type = ReaderAnnotationTool.highlight,
        title = Strings.pdf_annotation_toolbar_highlight,
        image = Drawables.annotate_highlight,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.underline,
        title = Strings.pdf_annotation_toolbar_underline,
        image = Drawables.annotate_underline,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.note,
        title = Strings.pdf_annotation_toolbar_note,
        image = Drawables.annotate_note,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.text,
        title = Strings.pdf_annotation_toolbar_text,
        image = Drawables.annotate_text,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.image,
        title = Strings.pdf_annotation_toolbar_image,
        image = Drawables.annotate_area,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.ink,
        title = Strings.pdf_annotation_toolbar_ink,
        image = Drawables.annotate_ink,
        isHidden = false
    ),
    ReaderTool(
        type = ReaderAnnotationTool.eraser,
        title = Strings.pdf_annotation_toolbar_eraser,
        image = Drawables.annotate_eraser,
        isHidden = false
    )
)

/** The system strip at the top of the screen (status bar, camera cutout), also while it is hidden. */
@Composable
private fun topSystemInset(): WindowInsets {
    return WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout)
}

/** The height of the toolbar when it is pinned in place of the top bar, including the status bar strip. */
@Composable
internal fun pinnedToolbarHeight(): Dp {
    val density = LocalDensity.current
    return with(density) { topSystemInset().getTop(density).toDp() } + TOOLBAR_PINNED_CONTENT_HEIGHT
}

private fun toolbarHeight(toolCount: Int): Dp {
    return TOOLBAR_CHROME_HEIGHT + TOOLBAR_TOOL_HEIGHT * toolCount
}

private fun distanceToRect(point: Offset, topLeft: Offset, size: Size): Float {
    val dx = max(max(topLeft.x - point.x, 0f), point.x - (topLeft.x + size.width))
    val dy = max(max(topLeft.y - point.y, 0f), point.y - (topLeft.y + size.height))
    return sqrt(dx * dx + dy * dy)
}

/**
 * Press and hold anywhere on the element to pick it up and drag it. The touch is observed in the
 * Initial pass, so a quick swipe is left to children (e.g. a scrolling tool row), while once the hold
 * is recognised the following movement is consumed before children can react to it.
 */
private fun Modifier.holdToDrag(
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var cancelled = false
        val heldLongEnough = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null
                    || change.changedToUpIgnoreConsumed()
                    || (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                ) {
                    cancelled = true
                    break
                }
            }
        } == null
        if (cancelled || !heldLongEnough) return@awaitEachGesture

        onDragStart(down.position)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                onDragCancel()
                return@awaitEachGesture
            }
            if (change.changedToUpIgnoreConsumed()) {
                change.consume()
                onDragEnd()
                return@awaitEachGesture
            }
            onDrag(change, change.positionChange())
        }
    }
}

@Composable
internal fun BoxScope.ReaderAnnotationCreationToolbar(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    position: ReaderToolbarPosition,
    onPositionChange: (ReaderToolbarPosition) -> Unit,
    containerSize: IntSize,
    onSnapTargetAreasVisibilityChange: (Boolean) -> Unit,
    shouldShowSnapTargetAreas: Boolean,
    isOverlayMode: Boolean,
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val roundCornerShape = RoundedCornerShape(size = 12.dp)
    val snapAreaBackgroundColor = MaterialTheme.colorScheme.surface

    val toolsList =
        if (viewState.fileType == ReaderFileType.PDF && !viewState.readingModeEnabled) {
            pdfReaderToolsList
        } else {
            htmlEpubReaderToolsList
        }
    val verticalHeight = toolbarHeight(toolsList.size)

    val insetPx = with(density) {
        if (viewState.showSideBar && isOverlayMode) TOOLBAR_OVERLAY_SIDEBAR_INSET.toPx() else 0f
    }
    val marginPx = with(density) { TOOLBAR_MARGIN.toPx() }
    // The "ignoring visibility" insets keep reporting the bar height while the bars are hidden.
    val topSystemInsetPx = topSystemInset().getTop(density).toFloat()
    // The pinned toolbar covers the status bar strip and has the height of the top bar.
    val pinnedHeightPx = topSystemInsetPx + with(density) { TOOLBAR_PINNED_CONTENT_HEIGHT.toPx() }
    val pinnedHeight = with(density) { pinnedHeightPx.toDp() }
    // In full-screen mode (top bar hidden) the system bars are hidden too and a PDF/HTML page is drawn
    // edge to edge, so the toolbar must stay below the status bar / camera cutout strip itself.
    val isTopBarAbsent = viewState.isPdfOrHtml() && !viewState.isTopBarVisible
    // The screen y of the bottom edge of the top bar, where the Start, End and Top toolbars begin. While
    // the toolbar is pinned they begin below it, as if the top bar was there.
    val belowTopBarY = if (isTopBarAbsent && position != ReaderToolbarPosition.Pinned) {
        topSystemInsetPx
    } else {
        pinnedHeightPx
    }
    // The screen y of the top of the box this toolbar is drawn in. It is the top bar's bottom edge, except
    // when there is no top bar (full screen, or the toolbar is pinned in its place).
    val containerTopY =
        if (isTopBarAbsent || position == ReaderToolbarPosition.Pinned) 0f else pinnedHeightPx
    // The pinned position replaces the top bar, so there is nothing to replace in full screen.
    val isPinnedAvailable = !isTopBarAbsent || position == ReaderToolbarPosition.Pinned
    val availablePositions = ReaderToolbarPosition.entries.filter {
        it != ReaderToolbarPosition.Pinned || isPinnedAvailable
    }
    // Docked at the top the toolbar spans the whole width, with no side margins.
    val horizontalWidth = with(density) { (containerSize.width - insetPx).toDp() }
        .coerceAtLeast(TOOLBAR_ITEM_SIZE)

    val sizeOf: (ReaderToolbarPosition) -> Size = { p ->
        with(density) {
            when (p) {
                ReaderToolbarPosition.Top -> Size(horizontalWidth.toPx(), TOOLBAR_ITEM_SIZE.toPx())
                ReaderToolbarPosition.Pinned -> Size(horizontalWidth.toPx(), pinnedHeightPx)
                else -> Size(TOOLBAR_ITEM_SIZE.toPx(), verticalHeight.toPx())
            }
        }
    }
    // The targets are screen coordinates in y (and container coordinates in x), so they do not depend on
    // which position currently decides where the container starts.
    val targetOf: (ReaderToolbarPosition) -> Offset = { p ->
        val size = sizeOf(p)
        when (p) {
            ReaderToolbarPosition.Start -> Offset(insetPx + marginPx, belowTopBarY + marginPx)
            ReaderToolbarPosition.End -> Offset(
                containerSize.width - marginPx - size.width,
                belowTopBarY + marginPx
            )

            ReaderToolbarPosition.Top -> Offset(insetPx, belowTopBarY)
            ReaderToolbarPosition.Pinned -> Offset(insetPx, 0f)
        }
    }

    val currentSizeOf by rememberUpdatedState(sizeOf)
    val currentTargetOf by rememberUpdatedState(targetOf)
    val currentAvailablePositions by rememberUpdatedState(availablePositions)
    val currentPosition by rememberUpdatedState(position)
    val currentOnPositionChange by rememberUpdatedState(onPositionChange)
    val currentOnSnapAreasChange by rememberUpdatedState(onSnapTargetAreasVisibilityChange)

    val offset = remember { Animatable(targetOf(position), Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    var grabPoint by remember { mutableStateOf(Offset.Zero) }
    var selectedPosition by remember { mutableStateOf(position) }
    val moveSpec = tween<Offset>(durationMillis = 300, easing = FastOutSlowInEasing)

    val target = targetOf(position)
    LaunchedEffect(target) {
        if (!isDragging) {
            offset.animateTo(target, moveSpec)
        }
    }

    val nearestPosition: () -> ReaderToolbarPosition = {
        currentAvailablePositions.minBy { p ->
            distanceToRect(
                point = pointerPosition,
                topLeft = currentTargetOf(p),
                size = currentSizeOf(p)
            )
        }
    }

    val onDragStart: (Offset) -> Unit = { startPoint ->
        isDragging = true
        grabPoint = startPoint
        pointerPosition = offset.value + startPoint
        selectedPosition = currentPosition
        currentOnSnapAreasChange(true)
        coroutineScope.launch { offset.stop() }
    }
    val onDrag: (PointerInputChange, Offset) -> Unit = { change, dragAmount ->
        change.consume()
        pointerPosition += dragAmount
        // The toolbar is not clamped to the container: part of it may leave the screen so the
        // finger can reach the edges and corners where the snap areas are.
        val newOffset = pointerPosition - grabPoint
        coroutineScope.launch { offset.snapTo(newOffset) }
        selectedPosition = nearestPosition()
    }
    val onDragEnd: () -> Unit = {
        val best = nearestPosition()
        isDragging = false
        currentOnSnapAreasChange(false)
        currentOnPositionChange(best)
        coroutineScope.launch { offset.animateTo(currentTargetOf(best), moveSpec) }
    }
    val onDragCancel: () -> Unit = {
        isDragging = false
        currentOnSnapAreasChange(false)
        coroutineScope.launch {
            offset.animateTo(currentTargetOf(currentPosition), moveSpec)
        }
    }

    // The drag handle moves the toolbar immediately.
    val handleDragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
    }
    // Pressing and holding anywhere else on the toolbar also picks it up (like iOS), while quick
    // swipes still scroll the tool row.
    val holdDragModifier = Modifier.holdToDrag(
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
    )

    if (shouldShowSnapTargetAreas) {
        // Same look as iOS: translucent blue areas with a dashed border, the one the toolbar would snap
        // to is highlighted. The pinned and top areas are stacked, sharing the dashed line between them.
        val blue = CustomTheme.colors.zoteroBlueWithDarkMode
        availablePositions.forEach { p ->
            val areaTopLeft = targetOf(p)
            val areaSize = sizeOf(p)
            val isSelected = p == selectedPosition
            val areaColor = blue.copy(alpha = if (isSelected) 0.5f else 0.15f)
            val dashColor = blue.copy(alpha = if (isSelected) 1f else 0.5f)
            val isVerticalArea = p == ReaderToolbarPosition.Start || p == ReaderToolbarPosition.End
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            areaTopLeft.x.roundToInt(),
                            (areaTopLeft.y - containerTopY).roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { areaSize.width.toDp() },
                        height = with(density) { areaSize.height.toDp() },
                    )
                    .background(
                        color = areaColor,
                        shape = if (isVerticalArea) PLACEHOLDER_SHAPE else RectangleShape
                    )
                    .drawBehind {
                        val strokeWidth = PLACEHOLDER_DASH_WIDTH.toPx()
                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(PLACEHOLDER_DASH_ON.toPx(), PLACEHOLDER_DASH_OFF.toPx()),
                            0f
                        )
                        fun line(start: Offset, end: Offset) {
                            drawLine(
                                color = dashColor,
                                start = start,
                                end = end,
                                strokeWidth = strokeWidth,
                                pathEffect = dashEffect,
                            )
                        }

                        val topLeft = Offset(0f, 0f)
                        val topRight = Offset(size.width, 0f)
                        val bottomLeft = Offset(0f, size.height)
                        val bottomRight = Offset(size.width, size.height)
                        when (p) {
                            ReaderToolbarPosition.Start, ReaderToolbarPosition.End -> drawRoundRect(
                                color = dashColor,
                                cornerRadius = CornerRadius(PLACEHOLDER_CORNER_RADIUS.toPx()),
                                style = Stroke(width = strokeWidth, pathEffect = dashEffect),
                            )

                            // Left, right and top, plus the line shared with the top area below it.
                            ReaderToolbarPosition.Pinned -> {
                                line(topLeft, bottomLeft)
                                line(topRight, bottomRight)
                                line(topLeft, topRight)
                                line(bottomLeft, bottomRight)
                            }

                            // Left, right and bottom, the top is the pinned area's bottom line.
                            ReaderToolbarPosition.Top -> {
                                line(topLeft, bottomLeft)
                                line(topRight, bottomRight)
                                line(bottomLeft, bottomRight)
                            }
                        }
                    }
            )
        }
    }

    // Docked at the top the toolbar sits flush against the top bar, so its top corners are square.
    // The pinned toolbar is an edge to edge bar like the top bar it replaces.
    val surfaceShape = when (position) {
        ReaderToolbarPosition.Top -> RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )

        ReaderToolbarPosition.Pinned -> RoundedCornerShape(0.dp)
        else -> roundCornerShape
    }
    val surfaceModifier = Modifier
        .offset {
            IntOffset(
                x = offset.value.x.roundToInt(),
                y = (offset.value.y - containerTopY).roundToInt(),
            )
        }
        .background(
            color = snapAreaBackgroundColor,
            shape = surfaceShape
        )
        .clip(surfaceShape)

    if (position == ReaderToolbarPosition.Top || position == ReaderToolbarPosition.Pinned) {
        val isPinned = position == ReaderToolbarPosition.Pinned
        BoxWithConstraints(
            modifier = surfaceModifier
                .width(horizontalWidth)
                .height(if (isPinned) pinnedHeight else TOOLBAR_ITEM_SIZE)
                .then(holdDragModifier),
        ) {
            val toolbarWidth = maxWidth
            // The handle and the buttons form one group, centered across the whole toolbar width. The
            // row is at least as wide as the toolbar, so it centers the group when everything fits
            // and scrolls when it doesn't.
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    // The pinned toolbar extends under the status bar, the buttons stay below it.
                    .padding(top = if (isPinned) with(density) { topSystemInsetPx.toDp() } else 0.dp)
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = toolbarWidth),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = handleDragModifier) {
                    ReaderAnnotationCreationButton(
                        isEnabled = true,
                        iconInt = Drawables.drag_handle,
                    )
                }
                ReaderToolbarItems(
                    viewState = viewState,
                    viewModel = viewModel,
                    toolsList = toolsList,
                    isHorizontal = true,
                    isDragging = isDragging,
                )
            }
        }
    } else {
        Column(
            modifier = surfaceModifier
                .width(TOOLBAR_ITEM_SIZE)
                .height(verticalHeight)
                .then(holdDragModifier)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            ReaderToolbarItems(
                viewState = viewState,
                viewModel = viewModel,
                toolsList = toolsList,
                isHorizontal = false,
                isDragging = isDragging,
            )
            Box(modifier = handleDragModifier) {
                ReaderAnnotationCreationButton(
                    isEnabled = true,
                    iconInt = Drawables.drag_handle,
                )
            }
        }
    }
}

/**
 * Shows [text] as a tooltip on long press, except while the toolbar is being dragged: holding a
 * button also picks the toolbar up, and a tooltip popping up mid-drag would be noise.
 */
@Composable
private fun ToolbarTooltip(
    text: String,
    isDragging: Boolean,
    content: @Composable () -> Unit,
) {
    val tooltipState = rememberTooltipState()
    LaunchedEffect(isDragging) {
        if (isDragging) {
            tooltipState.dismiss()
        }
    }
    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            4.dp
        ),
        tooltip = {
            PlainTooltip() {
                Text(text = text)
            }
        },
        state = tooltipState,
        enableUserInput = !isDragging,
        content = content,
    )
}

@Composable
private fun ReaderToolbarItems(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    toolsList: List<ReaderTool>,
    isHorizontal: Boolean,
    isDragging: Boolean,
) {
    toolsList.forEach { tool ->
        if (!tool.isHidden) {
            ToolbarTooltip(
                text = safeStringResource(tool.title),
                isDragging = isDragging,
            ) {
                ReaderAnnotationCreationToggleButton(
                    activeAnnotationTool = viewState.activeTool,
                    pdfReaderTool = tool,
                    toggleButton = viewModel::toggle
                )
            }
        }
    }
    val activeAnnotationTool = viewState.activeTool
    if (activeAnnotationTool != null) {
        if (activeAnnotationTool == ReaderAnnotationTool.eraser) {
            ReaderEmptyFilterCircle(onClick = viewModel::showToolOptions)
        } else {
            val color = viewState.toolColors[activeAnnotationTool]
            if (color != null) {
                ReaderFilterCircle(hex = color, onClick = viewModel::showToolOptions)
            }
        }
    }

    if (isHorizontal) {
        Spacer(modifier = Modifier.width(TOOLBAR_HORIZONTAL_SPACER))
    } else {
        Spacer(modifier = Modifier.height(48.dp))
    }

    ToolbarTooltip(
        text = safeStringResource(Strings.accessibility_pdf_undo),
        isDragging = isDragging,
    ) {
        ReaderAnnotationCreationButton(
            isEnabled = viewState.canUndo,
            iconInt = Drawables.undo_24px,
            onButtonClick = viewModel::onUndoClick
        )
    }

    ToolbarTooltip(
        text = safeStringResource(Strings.accessibility_pdf_redo),
        isDragging = isDragging,
    ) {
        ReaderAnnotationCreationButton(
            isEnabled = viewState.canRedo,
            iconInt = Drawables.redo_24px,
            onButtonClick = viewModel::onRedoClick
        )
    }

    ToolbarTooltip(
        text = safeStringResource(Strings.cancel),
        isDragging = isDragging,
    ) {
        ReaderAnnotationCreationButton(
            isEnabled = true,
            iconInt = Drawables.cancel_24px,
            onButtonClick = viewModel::toggleToolbarButton
        )
    }
}