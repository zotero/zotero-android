package org.zotero.android.pdf.data

import android.graphics.PointF
import android.graphics.RectF
import com.pspdfkit.document.PdfDocument
import org.zotero.android.ktx.distance

class AnnotationBoundingBoxConverter constructor(
    val document: PdfDocument,
) {

    fun convertFromDb(rect: RectF, page: Int): RectF? {
        return document.pdfProjection.toPdfRect(rect, page)
    }

    fun convertFromDb(point: PointF, page: Int): PointF? {
        return document.pdfProjection.toNormalizedPoint(point, page)
    }

    fun convertToDb(rect: RectF, page: Int): RectF? {
        return document.pdfProjection.toRawRect(rect, page)
    }

    fun convertToDb(point: PointF, page: Int) : PointF? {
        return document.pdfProjection.toRawPoint(point, page)
    }

    fun sortIndexMinY(rect: RectF, page: Int): Float {
        val savedRotation = document.getPageRotation(page)
        val pageSize = document.getPageSize(page)
        when (savedRotation) {
            PdfDocument.NO_ROTATION -> {
                return pageSize.height - rect.top
            }
            PdfDocument.ROTATION_180 -> {
                return rect.bottom
            }
            PdfDocument.ROTATION_90 -> {
                return pageSize.width - rect.left
            }
            PdfDocument.ROTATION_270 -> {
                return rect.left
            }
            else -> {
                return -1.0f
            }
        }
    }

    fun textOffset(rect: RectF, page: Int) : Int? {
        var index = 0
        var minDistance = Float.MAX_VALUE
        var textOffset = 0

        val pageLength = document.getPageTextLength(page)

        if (pageLength == 0) {
            return null
        }

        for (i in 0 until pageLength) {
            val glyphRect = document.getPageTextRects(page, i, 1).firstOrNull() ?: continue
            val distance = rect.distance(glyphRect)
            if (distance < minDistance) {
                minDistance = distance
                textOffset = index
            }

            index += 1
        }
        return textOffset
    }
}