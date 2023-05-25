package org.zotero.android.pdf

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.AnnotationBoundingBoxCalculator
import org.zotero.android.sync.Tag

interface Annotation {
    val key: String
    val type: AnnotationType
    val lineWidth: Float?
    val page: Int
    val pageLabel: String
    val comment: String
    val color: String
    val text: String?
    val tags: List<Tag>


    fun paths(boundingBoxConverter: AnnotationBoundingBoxConverter) : List<List<PointF>>
    fun rects(boundingBoxConverter: AnnotationBoundingBoxConverter) : List<RectF>

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
            AnnotationType.note, AnnotationType.highlight , AnnotationType.image -> {
                val rects = rects(boundingBoxConverter = boundingBoxConverter)
                if (rects.size == 1) {
                    return rects[0].rounded(3)
                }
                return AnnotationBoundingBoxCalculator.boundingBox(rects).rounded(3)
            }
        }
    }

    val displayColor: String get(){
        if (!color.startsWith("#")) {
            return "#" + this.color
        }
        return this.color
    }

}