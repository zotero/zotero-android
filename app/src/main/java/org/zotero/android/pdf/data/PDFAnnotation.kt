package org.zotero.android.pdf.data

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.reader.AnnotationKey
import org.zotero.android.sync.AnnotationBoundingBoxCalculator
import org.zotero.android.sync.Library
import org.zotero.android.sync.Tag

interface PDFAnnotation {
    val key: String
    val readerKey: AnnotationKey
    val type: AnnotationType
    val lineWidth: Float?
    val page: Int
    val pageLabel: String
    val comment: String
    val color: String
    val text: String?
    val fontSize: Float?
    val rotation: Int?
    val sortIndex: String
    val tags: List<Tag>

    fun editability(currentUserId: Long, library: Library): AnnotationEditability
    fun paths(boundingBoxConverter: AnnotationBoundingBoxConverter) : List<List<PointF>>
    fun rects(boundingBoxConverter: AnnotationBoundingBoxConverter) : List<RectF>
    fun isAuthor(currentUserId: Long): Boolean

    fun boundingBox(boundingBoxConverter: AnnotationBoundingBoxConverter): RectF {
        when (this.type) {
            AnnotationType.ink -> {
                val paths = paths(boundingBoxConverter = boundingBoxConverter)
                val lineWidth = this.lineWidth ?: 1F
                return AnnotationBoundingBoxCalculator.boundingBox(
                    paths = paths,
                    lineWidth = lineWidth
                ).rounded(3)

            }

            AnnotationType.note, AnnotationType.highlight, AnnotationType.image, AnnotationType.underline, AnnotationType.text -> {
                val rects = rects(boundingBoxConverter = boundingBoxConverter)
                if (rects.size == 1) {
                    return rects[0].rounded(3)
                }
                return AnnotationBoundingBoxCalculator.boundingBox(rects).rounded(3)
            }
        }
    }

    fun author(displayName: String, username: String): String

    val displayColor: String get(){
        if (!color.startsWith("#")) {
            return "#" + this.color
        }
        return this.color
    }

}