package org.zotero.android.screens.htmlepub.reader.sidebar

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Annotations
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Outline

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
    thumbnailsLazyListState: LazyListState,
) {
    val selectorOptions =
        if (viewModel.isCurrentFilePdf()) {
            pdfSliderOptions
        } else {
            htmlEpubSliderOptions
        }.map {
            stringResource(id = it.optionStringId)
        }

    val selectedOption = viewState.sidebarSliderSelectedOption
    SecondaryTabRow(
        modifier = Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(top = 24.dp),
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
                viewModel = viewModel,
                viewState = viewState,
                thumbnailsLazyListState = thumbnailsLazyListState,
            )
        }
        Annotations -> {
            HtmlEpubReaderAnnotationsSidebar(
                viewModel = viewModel,
                viewState = viewState,
                annotationsLazyListState = annotationsLazyListState,
                layoutType = layoutType,
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
