package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions.Annotations
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions.Outline
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions.Thumbnails

private val sliderOptions = listOf(
    Thumbnails,
    Annotations,
    Outline
)

@Composable
internal fun PdfReaderSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
    annotationsLazyListState: LazyListState,
    thumbnailsLazyListState: LazyListState,
) {
    val selectorOptions = sliderOptions.map {
        stringResource(id = it.optionStringId)
    }

    val selectedOption = viewState.sidebarSliderSelectedOption

    SecondaryTabRow(
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
                onClick = { vMInterface.setSidebarSliderSelectedOption(index) }
            )
        }
    }

    when (selectedOption) {
        Thumbnails -> {
            PdfReaderThumbnailsSidebar(
                vMInterface = vMInterface,
                viewState = viewState,
                thumbnailsLazyListState = thumbnailsLazyListState,
            )
        }

        Annotations -> {
            PdfReaderAnnotationsSidebar(
                vMInterface = vMInterface,
                viewState = viewState,
                annotationsLazyListState = annotationsLazyListState,
                layoutType = layoutType,
            )
        }

        Outline -> {
            PdfReaderOutlineSidebar(
                vMInterface = vMInterface,
                viewState = viewState,
                layoutType = layoutType,
            )
        }
    }
}
