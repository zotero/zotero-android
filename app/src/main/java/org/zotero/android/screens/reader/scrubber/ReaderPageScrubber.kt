package org.zotero.android.screens.reader.scrubber

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.roundToInt

private val ScrubberThumbnailHeight = 44.dp
private val ScrubberPreviewWidth = 40.dp
private val ScrubberSelectedBorderWidth = 2.5.dp
private const val ScrubberWidthFraction = 0.8f

private const val AssumedPageAspectRatio = 0.72f

@Composable
internal fun ReaderPageIndicatorLabel(
    viewModel: ReaderScrubberViewModel = viewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(ReaderScrubberViewState())
    val selectedPage = viewState.selectedPage
    val pageCount = viewState.thumbnailCache.size

    AnimatedVisibility(
        visible = viewState.showPageLabel && selectedPage != null && pageCount > 0,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        if (selectedPage != null && pageCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                val baseStyle = MaterialTheme.typography.bodySmall
                Text(
                    text = "${selectedPage + 1} of $pageCount",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = baseStyle.copy(
                        fontSize = baseStyle.fontSize * 2,
                        lineHeight = baseStyle.lineHeight * 2,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ReaderPageScrubber(
    viewModel: ReaderScrubberViewModel = viewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(ReaderScrubberViewState())
    val pageCount = viewState.thumbnailCache.size
    val density = LocalDensity.current

    var contentWidthPx by remember { mutableIntStateOf(0) }
    var dragXPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints {
        val maxAllowedWidth = maxWidth * ScrubberWidthFraction
        val horizontalPillPadding = 8.dp

        val landmarkPageIndices = remember(maxAllowedWidth, pageCount) {
            val rowBudgetPx = with(density) { (maxAllowedWidth - horizontalPillPadding * 2).toPx() }
                .coerceAtLeast(0f)
                .roundToInt()
            val landmarkCount = computeLandmarkCount(rowBudgetPx, pageCount, density.density)
            computeLandmarkIndices(pageCount, landmarkCount)
        }

        LaunchedEffect(landmarkPageIndices) {
            if (landmarkPageIndices.isNotEmpty()) {
                viewModel.onLandmarksComputed(landmarkPageIndices)
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = maxAllowedWidth)
                .padding(bottom = 12.dp)
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            val fastLongPressViewConfiguration = rememberFastLongPressViewConfiguration()
            CompositionLocalProvider(LocalViewConfiguration provides fastLongPressViewConfiguration) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalPillPadding, vertical = 8.dp)
                        .onSizeChanged { contentWidthPx = it.width }
                        .pointerInput(pageCount) {
                            if (pageCount > 0) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        dragXPx = offset.x.coerceIn(0f, size.width.toFloat())
                                        viewModel.onScrubStart()
                                        viewModel.onScrubTo(
                                            pageFromX(
                                                offset.x,
                                                size.width,
                                                pageCount
                                            )
                                        )
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        dragXPx =
                                            change.position.x.coerceIn(0f, size.width.toFloat())
                                        viewModel.onScrubTo(
                                            pageFromX(
                                                change.position.x,
                                                size.width,
                                                pageCount
                                            )
                                        )
                                    },
                                    onDragEnd = { viewModel.onScrubEnd() },
                                    onDragCancel = { viewModel.onScrubEnd() },
                                )
                            }
                        }
                        .pointerInput(pageCount) {
                            if (pageCount > 0) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        viewModel.onTapAt(
                                            pageFromX(
                                                offset.x,
                                                size.width,
                                                pageCount
                                            )
                                        )
                                    },
                                )
                            }
                        },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        landmarkPageIndices.forEach { index ->
                            ScrubberThumbnail(
                                bitmap = viewState.thumbnailCache.getOrNull(index),
                                height = ScrubberThumbnailHeight,
                            )
                        }
                    }

                    val selectedPage = viewState.selectedPage
                    if (selectedPage != null) {
                        val previewWidthPx = with(density) { ScrubberPreviewWidth.toPx() }
                        val maxOffsetPx = (contentWidthPx - previewWidthPx).coerceAtLeast(0f)
                        val rawXPx = if (viewState.isScrubbing) {
                            dragXPx
                        } else {
                            pageToX(selectedPage, pageCount, contentWidthPx)
                        }
                        val previewOffsetPx =
                            (rawXPx - previewWidthPx / 2).coerceIn(0f, maxOffsetPx)

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset { IntOffset(previewOffsetPx.roundToInt(), 0) }
                                .width(ScrubberPreviewWidth)
                                .height(ScrubberThumbnailHeight)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = ScrubberSelectedBorderWidth,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            val previewBitmap = viewState.thumbnailCache.getOrNull(selectedPage)
                            if (previewBitmap == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CustomTheme.colors.secondaryContent,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Image(
                                    modifier = Modifier.fillMaxSize(),
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberFastLongPressViewConfiguration(): ViewConfiguration {
    val original = LocalViewConfiguration.current
    return remember(original) {
        object : ViewConfiguration by original {
            override val longPressTimeoutMillis: Long
                get() = original.longPressTimeoutMillis / 4
        }
    }
}

@Composable
private fun ScrubberThumbnail(
    bitmap: Bitmap?,
    height: Dp,
) {
    val modifier = Modifier
        .clip(shape = RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surface)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(32.dp)
                    .height(height),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = CustomTheme.colors.secondaryContent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            Image(
                modifier = Modifier
                    .height(height)
                    .aspectRatio(aspectRatio),
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
            )
        }
    }
}

private fun computeLandmarkCount(contentWidthPx: Int, pageCount: Int, densityValue: Float): Int {
    if (pageCount <= 0 || contentWidthPx <= 0) {
        return 0
    }
    if (pageCount == 1) {
        return 1
    }
    val assumedSlotWidthPx = ScrubberThumbnailHeight.value * AssumedPageAspectRatio * densityValue
    val spacingPx = 6.dp.value * densityValue
    val count = ((contentWidthPx + spacingPx) / (assumedSlotWidthPx + spacingPx)).toInt()
    return count.coerceIn(2, pageCount)
}

private fun computeLandmarkIndices(pageCount: Int, desiredCount: Int): List<Int> {
    if (pageCount <= 0 || desiredCount <= 0) {
        return emptyList()
    }
    val count = desiredCount.coerceIn(1, pageCount)
    if (count == 1) {
        return listOf(0)
    }
    return (0 until count).map { i ->
        ((i * (pageCount - 1).toFloat()) / (count - 1)).roundToInt()
    }.distinct()
}

private fun pageFromX(x: Float, widthPx: Int, pageCount: Int): Int {
    if (pageCount <= 0 || widthPx <= 0) {
        return 0
    }
    val fraction = (x / widthPx).coerceIn(0f, 1f)
    return (fraction * (pageCount - 1)).roundToInt().coerceIn(0, pageCount - 1)
}

private fun pageToX(page: Int, pageCount: Int, widthPx: Int): Float {
    if (pageCount <= 1 || widthPx <= 0) {
        return 0f
    }
    return (page.toFloat() / (pageCount - 1).toFloat()) * widthPx
}
