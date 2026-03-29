package org.zotero.android.screens.htmlepub.annotationmore


import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import org.zotero.android.screens.settings.elements.NewSettingsDivider

@Composable
internal fun HtmlEpubAnnotationMoreHighlightRow(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    HtmlEpubAnnotationMoreHighlightText(
        annotationColor = annotationColor,
        viewState = viewState,
        onValueChange = viewModel::onHighlightTextValueChange,
    )
    NewSettingsDivider()
    HtmlEpubAnnotationMoreColorPicker(viewState, viewModel)
}


@Composable
internal fun HtmlEpubAnnotationMoreNoteRow(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel,
) {
    HtmlEpubAnnotationMoreColorPicker(viewState, viewModel)
}


@Composable
internal fun HtmlEpubAnnotationMoreUnderlineRow(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel,
) {
    val annotationColor =
        Color(viewState.color.toColorInt())
    HtmlEpubAnnotationMoreUnderlineText(
        annotationColor = annotationColor,
        viewState = viewState,
        onValueChange = viewModel::onUnderlineTextValueChange,
    )
    NewSettingsDivider()
    HtmlEpubAnnotationMoreColorPicker(viewState, viewModel)
}