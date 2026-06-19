package org.zotero.android.database.requests

import android.graphics.PointF
import android.graphics.RectF
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RPath
import org.zotero.android.database.objects.RPathCoordinate
import org.zotero.android.database.objects.RRect
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.data.PDFDocumentAnnotation
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber

class CreatePDFAnnotationsDbRequestV2 @AssistedInject constructor(
    @Assisted("attachmentKey") private val attachmentKey: String,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("annotations") private val annotations: List<PDFDocumentAnnotation>,
    @Assisted("userId") private val userId: Long,

    schemaController: SchemaController,
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

        for (rect in rects) {

            val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
            rRect.minX = rect.left.toDouble()
            rRect.minY = rect.bottom.toDouble()
            rRect.maxX = rect.right.toDouble()
            rRect.maxY = rect.top.toDouble()
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

        paths.forEachIndexed { idx, path ->
            val rPath = database.createEmbeddedObject(RPath::class.java, item, "paths")
            rPath.sortIndex = idx
            path.forEachIndexed { idy, point ->
                val rXCoordinate =
                    database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rXCoordinate.value = point.x.toDouble()
                rXCoordinate.sortIndex = idy * 2

                val rYCoordinate =
                    database.createEmbeddedObject(RPathCoordinate::class.java, rPath, "coordinates")
                rYCoordinate.value = point.y.toDouble()
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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("attachmentKey") attachmentKey: String,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("annotations") annotations: List<PDFDocumentAnnotation>,
            @Assisted("userId") userId: Long,
        ): CreatePDFAnnotationsDbRequestV2
    }
}