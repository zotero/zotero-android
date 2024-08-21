package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions.Annotations
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions.Outline
import org.zotero.android.uicomponents.selector.MultiSelector
import org.zotero.android.uicomponents.selector.MultiSelectorOption
import org.zotero.android.uicomponents.theme.CustomTheme

private val sliderOptions = listOf(
//    Thumbnails,
    Annotations,
    Outline
)

@Composable
internal fun PdfReaderSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
    focusRequester: FocusRequester,
    lazyListState: LazyListState,
) {
    val selectorOptions = sliderOptions.map {
        MultiSelectorOption(
            id = it.ordinal, optionString = stringResource(id = it.optionStringId)
        )
    }

    val selectorColor = CustomTheme.colors.primaryContent
    val selectedOption = viewState.sidebarSliderSelectedOption
    Spacer(modifier = Modifier.height(16.dp))
    MultiSelector(
        modifier = Modifier
            .fillMaxWidth()
            .height(layoutType.calculateSelectorHeight())
            .padding(horizontal = 16.dp)
        ,
        options = selectorOptions,
        selectedOptionId = selectedOption.ordinal,
        onOptionSelect = vMInterface::setSidebarSliderSelectedOption,
        fontSize = layoutType.calculatePdfSettingsOptionTextSize(),
        selectedColor = selectorColor,
        unselectedcolor = selectorColor
    )

    when (selectedOption) {
        Annotations -> {
            PdfReaderAnnotationsSidebar(
                vMInterface = vMInterface,
                viewState = viewState,
                lazyListState = lazyListState,
                layoutType = layoutType,
                focusRequester = focusRequester,
            )
        }

        Outline -> {
            PdfReaderOutlineSidebar(
                vMInterface = vMInterface,
                viewState = viewState,
                layoutType = layoutType,
            )
        }

        else -> {
            //no-op
        }
    }
}
