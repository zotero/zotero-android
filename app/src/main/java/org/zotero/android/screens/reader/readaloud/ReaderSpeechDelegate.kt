package org.zotero.android.screens.reader.readaloud

import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.speech.SpeechManagerDelegate
import org.zotero.android.speech.data.SDTRect
import java.io.File

class ReaderSpeechDelegate(
    override val documentTitle: String?,
    override val documentFile: File?,
    private val fileType: ReaderFileType,
    private val currentPage: () -> Int,
    private val pagesCount: () -> Int?,
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

    override fun moved(pageIndex: Int, previousPageIndex: Int) {
    }

    override fun focusPage(pageIndex: Int) {
    }

    override fun readAloudHighlightChanged(
        text: String,
        rects: List<SDTRect>,
        pageIndex: Int,
        sourceLocation: Int,
        sourceTextLength: Int,
    ) {
    }

    override fun annotationPreviewChanged(
        text: String,
        rects: List<SDTRect>,
        pageIndex: Int,
        tool: ReaderAnnotationTool,
        color: String,
        sourceLocation: Int,
        sourceTextLength: Int,
    ) {
    }

    override fun createAnnotation(
        tool: ReaderAnnotationTool,
        color: String,
        text: String,
        rects: List<SDTRect>,
        pageIndex: Int,
        sourceLocation: Int,
        sourceTextLength: Int,
    ) {
    }

    override fun clearAnnotationPreview() {
    }
}