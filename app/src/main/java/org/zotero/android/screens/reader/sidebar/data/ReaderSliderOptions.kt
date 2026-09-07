package org.zotero.android.screens.reader.sidebar.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class ReaderSliderOptions(@StringRes val optionStringId: Int) {
    Thumbnails(Strings.pdf_reader_slider_thumbnails),
    Annotations(Strings.pdf_reader_slider_annotations),
    Outline(Strings.pdf_reader_slider_outline),
}