package org.zotero.android.pdf

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.key
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber

data class DatabaseAnnotation(
    private val item: RItem
): Annotation {

    val key: String get() {
        return this.item.key
    }

    val _color: String? get() {
        val color = this.item.fieldValue(FieldKeys.Item.Annotation.color)
        if (color == null) {
            Timber.e("DatabaseAnnotation: ${this.key} missing color!")
            return null
        }

        return color
    }

    val color: String get(){
        return this._color ?: "#000000"
    }

    fun isAuthor(currentUserId: Long): Boolean {
        return if (this.item.libraryId == LibraryIdentifier.custom(RCustomLibraryType.myLibrary)) {
            true
        } else {
            this.item.createdBy?.identifier == currentUserId
        }
    }

    fun editability(currentUserId: Long, library: Library): AnnotationEditability {
        when (library.identifier) {
            is LibraryIdentifier.custom -> {
                return if (library.metadataEditable) AnnotationEditability.editable else AnnotationEditability.notEditable
            }

            is LibraryIdentifier.group -> {
                if (!library.metadataEditable) {
                    return AnnotationEditability.notEditable
                }
                return if (isAuthor(currentUserId = currentUserId)) AnnotationEditability.editable else AnnotationEditability.deletable
            }
        }
    }

    fun author(displayName: String, username: String): String {
        val authorName =
            item.fields.where().key(FieldKeys.Item.Annotation.authorName).findFirst()?.value
        if (authorName != null) {
            return authorName
        }

        val createdBy = this.item.createdBy

        if (createdBy != null) {
            if (!createdBy.name.isEmpty()) {
                return createdBy.name
            }

            if (!createdBy.username.isEmpty()) {
                return createdBy.username
            }
        }

        if (!displayName.isEmpty()) {
            return displayName
        }

        if (!username.isEmpty()) {
            return username
        }

        return "Unknown"
    }

    val _type: AnnotationType? get() {
        val rawValue = this.item.fieldValue(FieldKeys.Item.Annotation.type)

        if (rawValue == null) {
            Timber.e("DatabaseAnnotation: ${this.key} missing annotation type!")
            return null
        }
        try {
            val type = AnnotationType.valueOf(rawValue)
            return type
        } catch (e: Exception) {
            Timber.w("DatabaseAnnotation: ${this.key} unknown annotation type ${rawValue}")
            return null
        }
    }

    val _page: Int? get() {
        val rawValue = this.item.fieldValue(FieldKeys.Item.Annotation.Position.pageIndex)
        if(rawValue == null) {
            Timber.e("DatabaseAnnotation: ${this.key} missing page!")
            return null
        }
        val page = rawValue.toIntOrNull()
        if (page == null) {
            Timber.e("DatabaseAnnotation: ${this.key} page incorrect format $rawValue")

            return rawValue.toDoubleOrNull()?.toInt()
        }
        return page
    }
    override val type: AnnotationType
        get() {
            return this._type ?: AnnotationType.note
        }
    override val lineWidth: Float?
        get() {
            return (this.item.fields.where().key(FieldKeys.Item.Annotation.Position.lineWidth)).findFirst()?.value?.toFloatOrNull()
        }
    override val page: Int
        get() {
            return this._page ?: 0
        }
    override val comment: String
        get() {
            return this.item.fieldValue(FieldKeys.Item.Annotation.comment) ?: ""
        }

    override fun paths(boundingBoxConverter: AnnotationBoundingBoxConverter): List<List<PointF>> {
        TODO("Not yet implemented")
    }

    override fun rects(boundingBoxConverter: AnnotationBoundingBoxConverter): List<RectF> {
        TODO("Not yet implemented")
    }

}