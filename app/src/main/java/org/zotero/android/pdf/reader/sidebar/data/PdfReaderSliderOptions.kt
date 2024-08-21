package org.zotero.android.pdf.reader.sidebar.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class PdfReaderSliderOptions(@StringRes val optionStringId: Int) {
    Thumbnails(Strings.pdf_reader_slider_thumbnails),
    Annotations(Strings.pdf_reader_slider_annotations),
    Outline(Strings.pdf_reader_slider_outline),
}