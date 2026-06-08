package org.zotero.android.screens.htmlepub.reader.sidebar

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.data.ReaderFileType
import org.zotero.android.screens.htmlepub.reader.sidebar.annotations.HtmlEpubReaderAnnotationsSidebar
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Annotations
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Outline
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Thumbnails
import org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.HtmlEpubReaderThumbnailsSidebar
import org.zotero.android.uicomponents.foundation.safeStringResource

private val htmlEpubSliderOptions = listOf(
    Annotations,
    Outline
)

private val pdfSliderOptions = listOf(
    Thumbnails,
    Annotations,
    Outline
)

@Composable
internal fun HtmlEpubReaderSidebar(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    annotationsLazyListState: LazyListState,
    annotationMaxSideSize: Int,
) {
    Column(Modifier) {
        val selectorOptions = if (viewState.fileType == ReaderFileType.PDF) {
            pdfSliderOptions
        } else {
            htmlEpubSliderOptions
        }
        val selectorOptionsStrings = selectorOptions
            .map {
                safeStringResource(id = it.optionStringId)
            }

        val selectedOption = viewState.sidebarSliderSelectedOption
        SecondaryTabRow(
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures {
                        //Prevent tap to be propagated to composables behind this screen.
                    }
                },
            selectedTabIndex = selectorOptions.indexOf(selectedOption)
        ) {
            selectorOptionsStrings.forEachIndexed { index, selectorOption ->
                val isSelected = selectorOptions.indexOf(selectedOption) == index
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
                    onClick = { viewModel.setSidebarSliderSelectedOption(selectorOptions[index]) }
                )
            }
        }

        when (selectedOption) {
            Thumbnails -> {
                HtmlEpubReaderThumbnailsSidebar(
                    annotationMaxSideSize = annotationMaxSideSize,
                )
            }

            Annotations -> {
                HtmlEpubReaderAnnotationsSidebar(
                    viewModel = viewModel,
                    viewState = viewState,
                    annotationsLazyListState = annotationsLazyListState,
                    annotationMaxSideSize = annotationMaxSideSize,
                )
            }

            Outline -> {
                HtmlEpubReaderOutlineSidebar(
                    viewModel = viewModel,
                    viewState = viewState,
                )
            }
        }
    }
}
