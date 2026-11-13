package org.zotero.android.speech

import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.speech.data.ReadAloudPosition
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
    fun structuredDocumentTextPage(pageIndex: Index): Int?
    fun readAloudLoaded(sdtPackBytes: ByteArray, packVersion: Int, schemaMajorVersion: Int) {
    }
    fun moved(pageIndex: Index, previousPageIndex: Index)
    fun focusPage(pageIndex: Index)
    fun readAloudHighlightChanged(position: ReadAloudPosition, pageIndex: Index)
    fun annotationPreviewChanged(
        position: ReadAloudPosition,
        pageIndex: Index,
        tool: ReaderAnnotationTool,
        color: String,
    )
    fun createAnnotation(
        tool: ReaderAnnotationTool,
        color: String,
        position: ReadAloudPosition,
        pageIndex: Index,
    )
    fun clearAnnotationPreview()
}