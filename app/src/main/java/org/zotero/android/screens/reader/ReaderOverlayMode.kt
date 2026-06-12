package org.zotero.android.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.reader.sidebar.ReaderSidebar

@Composable
internal fun ReaderOverlayMode(
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
    annotationsLazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    annotationMaxSideSize: Int,
) {
    val isPdfOrHtml = viewState.isPdfOrHtml()
    var boxModifier = Modifier.fillMaxSize()
    if (!isPdfOrHtml) {
        val density = LocalDensity.current
        val systemBarsInsets = WindowInsets.systemBarsIgnoringVisibility
        val statusBarTop = with(density) { systemBarsInsets.getTop(this).toDp() } + TopAppBarDefaults.TopAppBarExpandedHeight
        val insetBottom = with(density) { systemBarsInsets.getBottom(this).toDp()}

        boxModifier = boxModifier
            .padding(
                top = statusBarTop,
                bottom = insetBottom
            )
    }
    Box(
        modifier = boxModifier
    ) {
        ReaderBox(
            viewState = viewState,
            viewModel = viewModel,
        )
        AnimatedContent(
            targetState = viewState.showSideBar,
            transitionSpec = {
                readerSidebarTransitionSpec()
            }, label = ""
        ) { showSideBar ->
            if (showSideBar) {
                val isTablet = layoutType.isTablet()

                var modifier = Modifier
                    .fillMaxHeight()
                if (isTablet) {
                    modifier = modifier.width(330.dp)
                } else {
                    modifier = modifier.fillMaxWidth()
                }
                Box(modifier = modifier) {
                    ReaderSidebar(
                        viewState = viewState,
                        viewModel = viewModel,
                        annotationsLazyListState = annotationsLazyListState,
                        annotationMaxSideSize = annotationMaxSideSize,
                    )
                }


            }
        }
    }
}
