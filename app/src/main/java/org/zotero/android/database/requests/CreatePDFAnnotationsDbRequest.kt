package org.zotero.android.database.requests

import android.graphics.PointF
import android.graphics.RectF
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.RRect
import org.zotero.android.database.objects.RUser
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.data.PDFDatabaseAnnotation
import org.zotero.android.pdf.data.PDFDocumentAnnotation
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController

class CreatePDFAnnotationsDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
    private val annotations: List<PDFDocumentAnnotation>,
    private val userId: Long,
    private val schemaController: SchemaController,
    private val boundingBoxConverter: AnnotationBoundingBoxConverter,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val parent =
            database.where<RItem>().key(this.attachmentKey, this.libraryId).findFirst() ?: return

        for (annotation in this.annotations) {
            create(annotation = annotation, parent = parent, database)
        }
    }

    private fun create(annotation: PDFDocumentAnnotation, parent: RItem, database: Realm) {
        var fromRestore = false
        val item: RItem
        val _item = database.where<RItem>().key(annotation.key, this.libraryId).findFirst()
        if (_item != null) {
            if (!_item.deleted) {
                return
            }

            item = _item
            item.deleted = false
            fromRestore = true
        } else {
            // If item didn't exist, create it
            item = database.createObject<RItem>()
            item.key = annotation.key
            item.rawType = ItemTypes.annotation
            item.localizedType =
                this.schemaController.localizedItemType(itemType = ItemTypes.annotation) ?: ""
            item.libraryId = this.libraryId
            item.dateAdded = annotation.dateModified
        }

        item.annotationType = annotation.type.name
        item.syncState = ObjectSyncState.synced.name
        item.changeType = UpdatableChangeType.user.name
        item.htmlFreeContent = if (annotation.comment.isEmpty()) {
            null
        } else {
            annotation.comment.strippedRichTextTags
        }
        item.dateModified = annotation.dateModified
        item.parent = parent

        if (annotation.isAuthor) {
            item.createdBy = database.where<RUser>().equalTo("identifier", this.userId).findFirst()
        }

        val changes = mutableListOf<RItemChanges>(
            RItemChanges.parent,
            RItemChanges.fields,
            RItemChanges.type,
            RItemChanges.tags
        )
        addFields(annotation, item, database = database)
        addRects(
            rects = annotation.rects,
            fromRestore = fromRestore,
            item = item,
            changes = changes,
            database = database
        )
        addPaths(
            paths = annotation.paths,
            fromRestore = fromRestore,
            item = item,
            changes = changes,
            database = database
        )
        item.changes.add(RObjectChange.create(changes = changes))
    }

    private fun addRects(
        rects: List<RectF>,
        item: RItem,
        changes: MutableList<RItemChanges>,
        database: Realm,
        fromRestore: Boolean
    ) {
        if (fromRestore) {
            item.rects.deleteAllFromRealm()
            changes.add(RItemChanges.rects)
        }
        if (rects.isEmpty()) {
            return
        }

        val annotation = PDFDatabaseAnnotation.init(item = item) ?: return

        val page = annotation.page

        for (rect in rects) {
            val dbRect = this.boundingBoxConverter.convertToDb(rect = rect, page = page) ?: rect

            val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
            rRect.minX = dbRect.left.toDouble()
            rRect.minY = dbRect.bottom.toDouble()
            rRect.maxX = dbRect.right.toDouble()
            rRect.maxY = dbRect.top.toDouble()
        }
        changes.add(RItemChanges.rects)
    }

    private fun addPaths(
        paths: List<List<PointF>>,
        item: RItem,
        changes: MutableList<RItemChanges>,
        database: Realm,
        fromRestore: Boolean
    ) {
        if (fromRestore) {
            item.paths.deleteAllFromRealm()
            changes.add(RItemChanges.paths)
        }
        if (paths.isEmpty()) {
            return
        }
        val annotation = PDFDatabaseAnnotation.init(item = item) ?: return
        val page = annotation.page

        paths.forEachIndexed { idx, path ->
            val rPath = database.createEmbeddedObject(RPath::class.java, item, "paths")
            rPath.sortIndex = idx
            path.forEachIndexed { idy, point ->
                val dbPoint =
                    this.boundingBoxConverter.convertToDb(point = point, page = page) ?: point
                val rXCoordinate =
                    database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rXCoordinate.value = dbPoint.x.toDouble()
                rXCoordinate.sortIndex = idy * 2

                val rYCoordinate =
                    database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rYCoordinate.value = dbPoint.y.toDouble()
                rYCoordinate.sortIndex = (idy * 2) + 1
            }

        }

        changes.add(RItemChanges.paths)
    }

    private fun addFields(annotation: PDFAnnotation, item: RItem, database: Realm) {
        for (field in FieldKeys.Item.Annotation.allPDFFields(annotation.type)) {
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseKey
            rField.changed = true
            when {
                field.key == FieldKeys.Item.Annotation.type -> {
                    rField.value = annotation.type.name
                }

                field.key == FieldKeys.Item.Annotation.color -> {
                    rField.value = annotation.color
                }

                field.key == FieldKeys.Item.Annotation.comment -> {
                    rField.value = annotation.comment
                }

                field.key == FieldKeys.Item.Annotation.Position.pageIndex && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.page}"
                }

                field.key == FieldKeys.Item.Annotation.Position.lineWidth && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = annotation.lineWidth?.rounded(3)?.toString() ?: ""
                }

                field.key == FieldKeys.Item.Annotation.pageLabel -> {
                    rField.value = annotation.pageLabel
                }

                field.key == FieldKeys.Item.Annotation.sortIndex -> {
                    rField.value = annotation.sortIndex
                    item.annotationSortIndex = annotation.sortIndex
                }

                field.key == FieldKeys.Item.Annotation.text -> {
                    rField.value = annotation.text ?: ""
                }

                field.key == FieldKeys.Item.Annotation.Position.rotation && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.rotation ?: 0}"
                }

                field.key == FieldKeys.Item.Annotation.Position.fontSize && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.fontSize ?: 0}"
                }
            }
        }
    }

}