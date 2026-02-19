package org.zotero.android.screens.htmlepub.reader.sidebar

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Annotations
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions.Outline

private val sliderOptions = listOf(
    Annotations,
    Outline
)

@Composable
internal fun HtmlEpubReaderSidebar(
    viewModel: HtmlEpubReaderViewModel,
    viewState: HtmlEpubReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
    annotationsLazyListState: LazyListState,
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
                onClick = { viewModel.setSidebarSliderSelectedOption(index) }
            )
        }
    }

    when (selectedOption) {
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
