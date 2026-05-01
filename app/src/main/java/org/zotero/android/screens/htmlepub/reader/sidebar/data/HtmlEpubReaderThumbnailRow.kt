package org.zotero.android.screens.htmlepub.reader.sidebar.data

import java.util.UUID

data class HtmlEpubReaderThumbnailRow(
    private val id: String = UUID.randomUUID().toString(),
    private val width: Int,
    private val height: Int,
    private val updateCount: Int = 1,
) {
    fun copyAndUpdateLoadedState(): HtmlEpubReaderThumbnailRow {
        return copy(
            id = this.id,
            updateCount = this.updateCount + 1,
        )
    }
}
