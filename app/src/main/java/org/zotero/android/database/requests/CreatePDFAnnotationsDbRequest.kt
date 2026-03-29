package org.zotero.android.database.requests

import android.graphics.PointF
import android.graphics.RectF
import io.realm.Realm
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.RRect
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.PDFDatabaseAnnotation
import org.zotero.android.pdf.data.PDFDocumentAnnotation
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber

class CreatePDFAnnotationsDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
    private val annotations: List<PDFDocumentAnnotation>,
    private val userId: Long,
    private val schemaController: SchemaController,
    private val boundingBoxConverter: AnnotationBoundingBoxConverter,
) : CreateReaderAnnotationsDbRequest<PDFDocumentAnnotation>(attachmentKey = attachmentKey, libraryId = libraryId, annotations = annotations, userId = userId, schemaController = schemaController) {

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

    override fun addFields(annotation: PDFDocumentAnnotation, item: RItem, database: Realm) {
        super.addFields(annotation, item, database)

        for (field in FieldKeys.Item.Annotation.extraPDFFields(annotation.type)) {
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseKey
            rField.changed = true
            when {
                field.key == FieldKeys.Item.Annotation.Position.pageIndex && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.page}"
                }

                field.key == FieldKeys.Item.Annotation.Position.lineWidth && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = annotation.lineWidth?.rounded(3)?.toString() ?: ""
                }

                field.key == FieldKeys.Item.Annotation.pageLabel -> {
                    rField.value = annotation.pageLabel
                }

                field.key == FieldKeys.Item.Annotation.Position.rotation && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.rotation ?: 0}"
                }

                field.key == FieldKeys.Item.Annotation.Position.fontSize && field.baseKey == FieldKeys.Item.Annotation.position -> {
                    rField.value = "${annotation.fontSize ?: 0}"
                }

                else -> {
                    Timber.w("CreatePDFAnnotationsDbRequest: unknown field, assigning empty value - ${field.key}")
                    rField.value = ""
                }
            }
        }
    }

    override fun addAdditionalProperties(
        annotation: PDFDocumentAnnotation,
        fromRestore: Boolean,
        item: RItem,
        changes: MutableList<RItemChanges>,
        database: Realm
    ) {
        addRects(
            annotation.rects,
            fromRestore = fromRestore,
            item = item,
            changes = changes,
            database = database
        )
        addPaths(
            annotation.paths,
            fromRestore = fromRestore,
            item = item,
            changes = changes,
            database = database
        )
    }

}