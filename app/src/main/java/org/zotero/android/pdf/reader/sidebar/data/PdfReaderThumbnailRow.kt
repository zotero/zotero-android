package org.zotero.android.pdf.reader.sidebar.data

import java.util.UUID

data class PdfReaderThumbnailRow(
    private val id: String = UUID.randomUUID().toString(),
    private val updateCount: Int = 1,
    val title: String,
    val pageIndex: Int,
) {
    fun copyAndUpdateLoadedState(): PdfReaderThumbnailRow {
        return copy(
            id = this.id,
            updateCount = this.updateCount + 1,
            title = this.title
        )
    }
}
