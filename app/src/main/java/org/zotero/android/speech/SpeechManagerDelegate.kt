package org.zotero.android.speech

import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.speech.data.SDTRect
import java.io.File

interface SpeechManagerDelegate<Index : Any> {
    val documentTitle: String?
    val documentFile: File?
    val documentContentType: String?
    val documentPassword: String?
    fun getCurrentPageIndex(): Index
    fun getNextPageIndex(currentPageIndex: Index): Index?
    fun getPreviousPageIndex(currentPageIndex: Index): Index?
    fun pageIndex(structuredDocumentTextPage: Int): Index?
    fun moved(pageIndex: Index, previousPageIndex: Index)
    fun focusPage(pageIndex: Index)
    fun readAloudHighlightChanged(
        text: String,
        rects: List<SDTRect>,
        pageIndex: Index,
        sourceLocation: Int,
        sourceTextLength: Int,
    )
    fun annotationPreviewChanged(
        text: String,
        rects: List<SDTRect>,
        pageIndex: Index,
        tool: ReaderAnnotationTool,
        color: String,
        sourceLocation: Int,
        sourceTextLength: Int,
    )
    fun createAnnotation(
        tool: ReaderAnnotationTool,
        color: String,
        text: String,
        rects: List<SDTRect>,
        pageIndex: Index,
        sourceLocation: Int,
        sourceTextLength: Int,
    )
    fun clearAnnotationPreview()
}