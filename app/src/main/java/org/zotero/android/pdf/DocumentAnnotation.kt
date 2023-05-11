package org.zotero.android.pdf

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import java.util.Date

data class DocumentAnnotation(
    val key: String,
    override val type: AnnotationType,
    override val page: Int,
    val pageLabel: String,
    val rects: List<RectF>,
    val paths: List<List<PointF>>,
    override val lineWidth: Float?,
    val author : String,
    val isAuthor : Boolean,
    val color: String,
    override val comment: String,
    val text: String?,
    val sortIndex: String,
    val dateModified: Date,
): Annotation {
    val readerKey: AnnotationKey
        get() {
            return AnnotationKey(key = this.key, type = AnnotationKey.Kind.document)
        }

    override fun paths(boundingBoxConverter: AnnotationBoundingBoxConverter): List<List<PointF>> {
        return this.paths
    }

    override fun rects(boundingBoxConverter: AnnotationBoundingBoxConverter): List<RectF> {
        return this.rects
    }

//    fun isAuthor(currentUserId: Int): Boolean {
//        return this.isAuthor
//    }
//
//    fun author(displayName: String, username: String): String {
//        return this.author
//    }
}