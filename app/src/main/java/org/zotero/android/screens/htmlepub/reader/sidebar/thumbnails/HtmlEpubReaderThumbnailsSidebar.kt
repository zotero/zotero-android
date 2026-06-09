package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
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
import org.zotero.android.androidx.content.pxToDp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.abs

@Composable
internal fun HtmlEpubReaderThumbnailsSidebar(
    viewModel: HtmlEpubThumbnailsViewModel = viewModel(),
    annotationMaxSideSize: Int,
) {
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubThumbnailsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val thumbnailsLazyListState = rememberLazyListState()

    LaunchedEffect(thumbnailsLazyListState) {
        listenToScroll(thumbnailsLazyListState, viewModel)
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            is HtmlEpubThumbnailsViewEffect.ScrollThumbnailListToIndex -> {
                val visibleItemsInfo = thumbnailsLazyListState.layoutInfo.visibleItemsInfo
                val scrollToIndex = consumedEffect.scrollToIndex
                if (visibleItemsInfo.isNotEmpty() && (scrollToIndex < visibleItemsInfo.first().index || scrollToIndex > visibleItemsInfo.last().index)) {
                    thumbnailsLazyListState.animateScrollToItem(index = scrollToIndex)
                }
            }
            else -> {
                //no-op
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = thumbnailsLazyListState,
            verticalArrangement = Arrangement.Absolute.spacedBy(16.dp),
        ) {
            items(
                viewState.thumbnailCache.size
            ) { index ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isSelected = viewState.isThumbnailSelected(index)
                    val horizontalPadding = if (isSelected) 13.dp else 16.dp
                    var rowModifier: Modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .clip(shape = RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)

                    if (isSelected) {
                        rowModifier = rowModifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Column(
                        modifier = rowModifier
                            .safeClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    viewModel.selectThumbnail(index)
                                },
                            )
                    ) {
                        val cachedBitmap =  viewState.thumbnailCache.getOrNull(index)
                        if (cachedBitmap == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.7f)
                                    .height(annotationMaxSideSize.pxToDp()),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = CustomTheme.colors.secondaryContent,
                                    strokeWidth = 2.dp,
                                )
                            }
                        } else {
                            Image(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = 0.7f)
                                    .height(annotationMaxSideSize.pxToDp()),
                                bitmap = cachedBitmap.asImageBitmap(),
                                contentDescription = null,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewState.pageLabels.getOrNull(index) ?: "Loading...",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
            }
        }
    }
}

private suspend fun listenToScroll(
    thumbnailsLazyListState: LazyListState,
    viewModel: HtmlEpubThumbnailsViewModel
) {
    snapshotFlow {
        val layoutInfo = thumbnailsLazyListState.layoutInfo
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
