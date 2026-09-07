package org.zotero.android.screens.reader.sidebar

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
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.sidebar.annotations.ReaderAnnotationsSidebar
import org.zotero.android.screens.reader.sidebar.data.ReaderSliderOptions
import org.zotero.android.screens.reader.sidebar.thumbnails.ReaderThumbnailsSidebar
import org.zotero.android.uicomponents.foundation.safeStringResource

private val htmlEpubSliderOptions = listOf(
    ReaderSliderOptions.Annotations,
    ReaderSliderOptions.Outline
)

private val pdfSliderOptions = listOf(
    ReaderSliderOptions.Thumbnails,
    ReaderSliderOptions.Annotations,
    ReaderSliderOptions.Outline
)

@Composable
internal fun ReaderSidebar(
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
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
            ReaderSliderOptions.Thumbnails -> {
                ReaderThumbnailsSidebar(
                    annotationMaxSideSize = annotationMaxSideSize,
                )
            }

            ReaderSliderOptions.Annotations -> {
                ReaderAnnotationsSidebar(
                    viewModel = viewModel,
                    viewState = viewState,
                    annotationsLazyListState = annotationsLazyListState,
                    annotationMaxSideSize = annotationMaxSideSize,
                )
            }

            ReaderSliderOptions.Outline -> {
                ReaderOutlineSidebar(
                    viewModel = viewModel,
                    viewState = viewState,
                )
            }
        }
    }
}
