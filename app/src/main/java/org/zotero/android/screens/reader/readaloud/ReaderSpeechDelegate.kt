package org.zotero.android.screens.reader.readaloud

import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.speech.SpeechManagerDelegate
import org.zotero.android.speech.data.ReadAloudPosition
import java.io.File

class ReaderSpeechDelegate(
    override val documentTitle: String?,
    override val documentFile: File?,
    private val fileType: ReaderFileType,
    private val currentPage: () -> Int,
    private val pagesCount: () -> Int?,
    private val bridge: ReaderReadAloudBridge?,
) : SpeechManagerDelegate<Int> {

    override val documentContentType: String?
        get() = when (fileType) {
            ReaderFileType.PDF -> "application/pdf"
            ReaderFileType.EPUB -> "application/epub+zip"
            ReaderFileType.HTML -> "text/html"
        }

    override val documentPassword: String? = null

    override fun getCurrentPageIndex(): Int = currentPage()

    override fun getNextPageIndex(currentPageIndex: Int): Int? {
        val count = pagesCount() ?: return null
        return if (currentPageIndex + 1 < count) currentPageIndex + 1 else null
    }

    override fun getPreviousPageIndex(currentPageIndex: Int): Int? {
        return if (currentPageIndex > 0) currentPageIndex - 1 else null
    }

    override fun pageIndex(structuredDocumentTextPage: Int): Int? {
        if (structuredDocumentTextPage < 0) return null
        val count = pagesCount()
        if (count != null && structuredDocumentTextPage >= count) return null
        return structuredDocumentTextPage
    }

    override fun structuredDocumentTextPage(pageIndex: Int): Int? {
        return pageIndex.takeIf { it >= 0 }
    }

    override fun moved(pageIndex: Int, previousPageIndex: Int) {
    }

    override fun focusPage(pageIndex: Int) {
    }

    override fun readAloudLoaded(sdtPackBytes: ByteArray, packVersion: Int, schemaMajorVersion: Int) {
        if (fileType == ReaderFileType.PDF) return
        bridge?.setSDTPack(sdtPackBytes, packVersion, schemaMajorVersion)
    }

    override fun readAloudHighlightChanged(position: ReadAloudPosition, pageIndex: Int) {
        if (position !is ReadAloudPosition.HtmlEpub) return
        bridge?.setSpotlight(position.sdtStart, position.sdtEnd)
    }

    fun clearSpeechHighlight() {
        bridge?.clearSpotlight()
    }

    override fun annotationPreviewChanged(
        position: ReadAloudPosition,
        pageIndex: Int,
        tool: ReaderAnnotationTool,
        color: String,
    ) {
        if (position !is ReadAloudPosition.HtmlEpub) return
        bridge?.setAnnotation(tool, color, position.sdtStart, position.sdtEnd)
    }

    override fun createAnnotation(
        tool: ReaderAnnotationTool,
        color: String,
        position: ReadAloudPosition,
        pageIndex: Int,
    ) {
        bridge?.endAnnotationSession(store = true)
    }

    override fun clearAnnotationPreview() {
        bridge?.endAnnotationSession(store = false)
    }
}