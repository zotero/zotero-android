package org.zotero.android.screens.htmlepub.reader.sidebar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.annotations.HtmlEpubReaderAnnotationsSidebar
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Annotations
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Outline
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.HtmlEpubReaderThumbnailsSidebar
import org.zotero.android.uicomponents.foundation.safeStringResource

private val htmlEpubSliderOptions = listOf(
    Annotations,
    Outline
)

private val pdfSliderOptions = listOf(
    HtmlEpubReaderSliderOptions.Thumbnails,
    Annotations,
    Outline
)

@Composable
internal fun HtmlEpubReaderSidebar(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
    annotationsLazyListState: LazyListState,
    annotationMaxSideSize: Int,
) {
    val selectorOptions =
        if (viewModel.isCurrentFilePdf()) {
            pdfSliderOptions
        } else {
            htmlEpubSliderOptions
        }.map {
            safeStringResource(id = it.optionStringId)
        }

    val selectedOption = viewState.sidebarSliderSelectedOption
    SecondaryTabRow(
        modifier = Modifier,
        selectedTabIndex = selectedOption.ordinal) {
        selectorOptions.forEachIndexed { index, selectorOption ->
            val isSelected = selectedOption.ordinal == index
            Tab(
                text = {
                    val textColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = selectorOption,
                        color = textColor,
                        style = MaterialTheme.typography.titleSmall,
                    )

                },
                selected = isSelected,
                onClick = { viewModel.setSidebarSliderSelectedOption(index) }
            )
        }
    }

    when (selectedOption) {
        HtmlEpubReaderSliderOptions.Thumbnails -> {
            HtmlEpubReaderThumbnailsSidebar(
                annotationMaxSideSize = annotationMaxSideSize,
                currentPage = viewState.currentPdfPageIndex,
                numOfPages = viewState.numOfPages,
                pageLabels = viewState.pageLabels,
            )
        }
        Annotations -> {
            HtmlEpubReaderAnnotationsSidebar(
                viewModel = viewModel,
                viewState = viewState,
                annotationsLazyListState = annotationsLazyListState,
                layoutType = layoutType,
                annotationMaxSideSize = annotationMaxSideSize,
            )
        }

        Outline -> {
            HtmlEpubReaderOutlineSidebar(
                viewModel = viewModel,
                viewState = viewState,
                layoutType = layoutType,
            )
        }
    }
}
