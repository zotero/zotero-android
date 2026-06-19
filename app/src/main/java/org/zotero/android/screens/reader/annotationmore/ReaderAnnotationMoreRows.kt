package org.zotero.android.screens.reader.annotationmore

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun ReaderAnnotationMoreHighlightRow(
    viewState: ReaderAnnotationMoreViewState,
    viewModel: ReaderAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    ReaderAnnotationMoreHighlightText(
        annotationColor = annotationColor,
        viewState = viewState,
        onValueChange = viewModel::onHighlightTextValueChange,
    )
    NewSettingsDivider()
    ReaderAnnotationMoreColorPicker(viewState = viewState, viewModel = viewModel)
}


@Composable
internal fun ReaderAnnotationMoreNoteRow(
    viewState: ReaderAnnotationMoreViewState,
    viewModel: ReaderAnnotationMoreViewModel,
) {
    ReaderAnnotationMoreColorPicker(viewState = viewState, viewModel = viewModel)
}


@Composable
internal fun ReaderAnnotationMoreUnderlineRow(
    viewState: ReaderAnnotationMoreViewState,
    viewModel: ReaderAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    ReaderAnnotationMoreUnderlineText(
        annotationColor = annotationColor,
        viewState = viewState,
        onValueChange = viewModel::onUnderlineTextValueChange,
    )
    NewSettingsDivider()
    ReaderAnnotationMoreColorPicker(viewState = viewState, viewModel = viewModel)
}

@Composable
internal fun ReaderAnnotationMoreImageRow(
    viewState: ReaderAnnotationMoreViewState,
    viewModel: ReaderAnnotationMoreViewModel,
) {
    ReaderAnnotationMoreColorPicker(viewState = viewState, viewModel = viewModel)
}

@Composable
internal fun ReaderAnnotationMoreFreeTextRow(
    viewState: ReaderAnnotationMoreViewState,
    viewModel: ReaderAnnotationMoreViewModel,
) {
    ReaderAnnotationMoreFontSizeSelector(
        viewState = viewState,
        viewModel = viewModel,
    )
    NewSettingsDivider()
    ReaderAnnotationMoreColorPicker(viewState = viewState, viewModel = viewModel)
}
