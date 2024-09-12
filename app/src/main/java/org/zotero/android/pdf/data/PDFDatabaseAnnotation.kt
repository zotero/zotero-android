package org.zotero.android.pdf.data

import android.graphics.PointF
import android.graphics.RectF
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.key
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.reader.AnnotationKey
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import timber.log.Timber
import kotlin.math.roundToInt

data class PDFDatabaseAnnotation(
    val item: RItem,
    override val type: AnnotationType
): PDFAnnotation {

    companion object {
        fun init(item: RItem): PDFDatabaseAnnotation? {
            val type: AnnotationType
            try {
                type = AnnotationType.valueOf(item.annotationType)
            } catch (e: Exception) {
                Timber.w("DatabaseAnnotation: ${item.key} unknown annotation type ${item.annotationType}")
                return null
            }
            if (!AnnotationsConfig.supported.contains(type.kind)) {
                return null
            }
            return PDFDatabaseAnnotation(item = item, type = type)
        }
    }

    override val readerKey: AnnotationKey
        get() {
            return AnnotationKey(key = this.key, type = AnnotationKey.Kind.document)
        }

    override val key: String get() {
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

    override val color: String get(){
        return this._color ?: "#000000"
    }
    override val text: String?
        get() {
            return this.item.fields.where().key(FieldKeys.Item.Annotation.text).findFirst()?.value
        }

    override val fontSize: Float?
        get() {
            return item.fields.where().key(FieldKeys.Item.Annotation.Position.fontSize).findFirst()?.value?.toFloatOrNull()
        }
    override val rotation: Int?
        get() {
            val rotation = item.fields.where().key(FieldKeys.Item.Annotation.Position.rotation).findFirst()?.value?.toDoubleOrNull() ?: return null
            return rotation.roundToInt()
        }

    override val sortIndex: String
        get() = this.item.annotationSortIndex

    override val tags: List<Tag>
        get() {
            return this.item.tags!!.map { Tag(it) }
        }

    override fun isAuthor(currentUserId: Long): Boolean {
        return if (this.item.libraryId == LibraryIdentifier.custom(RCustomLibraryType.myLibrary)) {
            true
        } else {
            this.item.createdBy?.identifier == currentUserId
        }
    }

    override fun editability(currentUserId: Long, library: Library): AnnotationEditability {
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

    override fun author(displayName: String, username: String): String {
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

    override val lineWidth: Float?
        get() {
            return (this.item.fields.where().key(FieldKeys.Item.Annotation.Position.lineWidth)).findFirst()?.value?.toFloatOrNull()
        }
    override val page: Int
        get() {
            return this._page ?: 0
        }
    override val pageLabel: String
        get() {
            return this._pageLabel ?: ""
        }

    val _pageLabel: String? get() {
        val label = this.item.fieldValue(FieldKeys.Item.Annotation.pageLabel)
        if (label == null) {
            Timber.e("DatabaseAnnotation: ${this.key} missing page label!")
            return null
        }
        return label
    }

    override val comment: String
        get() {
            return this.item.fieldValue(FieldKeys.Item.Annotation.comment) ?: ""
        }

    override fun paths(boundingBoxConverter: AnnotationBoundingBoxConverter): List<List<PointF>> {
        val page = this._page ?: return emptyList()
        val pageIndex = page
        val paths = mutableListOf<List<PointF>>()
        for (path in this.item.paths.sort("sortIndex")) {
            if (path.coordinates.size % 2 != 0) {
                continue
            }
            val sortedCoordinates = path.coordinates.sort("sortIndex")
            val lines = (0 until (path.coordinates.size / 2)).mapNotNull { idx ->
                val point = PointF(
                    sortedCoordinates[idx * 2]!!.value.toFloat(),
                    sortedCoordinates[(idx * 2) + 1]!!.value.toFloat()
                )
                boundingBoxConverter.convertFromDb(point, pageIndex)?.rounded(3)
            }
            paths.add(lines)
        }
        return paths
    }

    override fun rects(boundingBoxConverter: AnnotationBoundingBoxConverter): List<RectF> {
        val page = this._page ?: return emptyList()
        return this.item.rects.map {
            RectF(
                /* left = */ it.minX.toFloat(),
                /* top = */ it.maxY.toFloat(),
                /* right = */ it.maxX.toFloat(),
                /* bottom = */ it.minY.toFloat(),
            )
        }.mapNotNull {
            boundingBoxConverter.convertFromDb(it, page)?.rounded(3)
        }
    }

}