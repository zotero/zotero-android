package org.zotero.android.screens.reader.scrubber

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.abs

private val ScrubberThumbnailHeight = 44.dp
private val ScrubberSelectedBorderWidth = 2.5.dp
private const val ScrubberWidthFraction = 0.8f

@Composable
internal fun ReaderPageScrubber(
    viewModel: ReaderScrubberViewModel = viewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(ReaderScrubberViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val scrubberLazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val currentSelectedPage = viewState.selectedPage
        if (currentSelectedPage != null) {
            centerOnIndex(scrubberLazyListState, currentSelectedPage, animate = false)
        }
    }

    LaunchedEffect(scrubberLazyListState) {
        listenToScroll(scrubberLazyListState, viewModel)
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            is ReaderScrubberViewEffect.ScrollScrubberListToIndex -> {
                centerOnIndex(scrubberLazyListState, consumedEffect.scrollToIndex)
            }

            else -> {
                //no-op
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(ScrubberWidthFraction)
            .padding(bottom = 12.dp)
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = scrubberLazyListState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(
                viewState.thumbnailCache.size
            ) { index ->
                val isSelected = viewState.isThumbnailSelected(index)
                var itemModifier = Modifier
                    .clip(shape = RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)

                if (isSelected) {
                    itemModifier = itemModifier.border(
                        width = ScrubberSelectedBorderWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    )
                }

                Box(
                    modifier = itemModifier
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                viewModel.selectThumbnail(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    val cachedBitmap = viewState.thumbnailCache.getOrNull(index)
                    if (cachedBitmap == null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(32.dp)
                                .height(ScrubberThumbnailHeight),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CustomTheme.colors.secondaryContent,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        val aspectRatio = cachedBitmap.width.toFloat() / cachedBitmap.height.toFloat()
                        Image(
                            modifier = Modifier
                                .height(ScrubberThumbnailHeight)
                                .aspectRatio(aspectRatio),
                            bitmap = cachedBitmap.asImageBitmap(),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun centerOnIndex(listState: LazyListState, index: Int, animate: Boolean = true) {
    if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
        listState.scrollToItem(index = index)
    }
    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
    val itemCenter = itemInfo.offset + itemInfo.size / 2
    val delta = (itemCenter - viewportCenter).toFloat()
    if (animate) {
        listState.animateScrollBy(delta)
    } else {
        listState.scrollBy(delta)
    }
}

private suspend fun listenToScroll(
    scrubberLazyListState: LazyListState,
    viewModel: ReaderScrubberViewModel
) {
    snapshotFlow {
        val layoutInfo = scrubberLazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo

        if (visibleItems.isEmpty()) {
            null
        } else {
            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

            visibleItems.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }?.index
        }
    }.collect { centerIndex ->
        if (centerIndex != null) {
            viewModel.requestThumbnail(centerIndex)
        }
    }
}
