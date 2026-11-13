package org.zotero.android.screens.reader.readaloud

import org.zotero.android.screens.reader.data.ReaderAnnotationTool

interface ReaderReadAloudBridge {
    fun setSDTPack(bytes: ByteArray, packVersion: Int, schemaMajorVersion: Int)
    fun setSpotlight(sdtStart: List<Int>, sdtEnd: List<Int>)
    fun clearSpotlight()
    fun setAnnotation(tool: ReaderAnnotationTool, color: String, sdtStart: List<Int>, sdtEnd: List<Int>)
    fun endAnnotationSession(store: Boolean)
}