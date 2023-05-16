package org.zotero.android.pdf

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.sync.Tag
import java.util.Date

data class DocumentAnnotation(
    override val key: String,
    override val type: AnnotationType,
    override val page: Int,
    override val pageLabel: String,
    val rects: List<RectF>,
    val paths: List<List<PointF>>,
    override val lineWidth: Float?,
    val author : String,
    val isAuthor : Boolean,
    override val color: String,
    override val comment: String,
    override val text: String?,
    val sortIndex: String,
    val dateModified: Date,
): Annotation {
    val readerKey: AnnotationKey
        get() {
            return AnnotationKey(key = this.key, type = AnnotationKey.Kind.document)
        }
    override val tags: List<Tag>
        get() = emptyList()

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