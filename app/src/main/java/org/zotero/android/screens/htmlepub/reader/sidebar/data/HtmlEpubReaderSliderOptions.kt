package org.zotero.android.screens.htmlepub.reader.sidebar.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class HtmlEpubReaderSliderOptions(@StringRes val optionStringId: Int) {
    Annotations(Strings.pdf_reader_slider_annotations),
    Outline(Strings.pdf_reader_slider_outline),
}