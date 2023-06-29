package org.zotero.android.screens.tagpicker.data

import org.zotero.android.sync.Tag

data class TagPickerResult(val tags: List<Tag>, val callPoint: CallPoint) {
    enum class CallPoint {
        PdfFilter, ItemDetails, AddNote
    }
}
