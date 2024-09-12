package org.zotero.android.sync

import android.graphics.RectF
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.UnderlineAnnotation

class ZoteroSquareAnnotations(pageIndex: Int, rect: RectF) :
    SquareAnnotation(pageIndex, rect) {
}

class ZoteroHighlightAnnotation(pageIndex: Int, rects: List<RectF>) :
    HighlightAnnotation(pageIndex, rects)

class ZoteroNoteAnnotation(pageIndex: Int, annotationRect: RectF, contents: String) :
    NoteAnnotation(pageIndex, annotationRect, contents, null)

class ZoteroUnderlineAnnotation(pageIndex: Int, rects: List<RectF>) :
    UnderlineAnnotation(pageIndex, rects)


