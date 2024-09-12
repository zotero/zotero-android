package org.zotero.android.database.requests

import android.graphics.RectF
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RRect
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.PDFDatabaseAnnotation
import org.zotero.android.sync.LibraryIdentifier

class EditAnnotationRectsDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val rects: List<RectF>,
    private val boundingBoxConverter: AnnotationBoundingBoxConverter,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return
        val annotation = PDFDatabaseAnnotation.init(item = item)?: return
        val page = annotation.page
        val dbRects =
            this.rects.map { this.boundingBoxConverter.convertToDb(rect = it, page = page) ?: it }
        if (!rects(dbRects, item.rects)) {
            return
        }
        sync(rects = dbRects, item = item, database = database)
    }

    private fun sync(rects: List<RectF>, item: RItem, database: Realm) {
        item.rects.deleteAllFromRealm()

        for (rect in rects) {
            val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
            rRect.minX = rect.left.toDouble()
            rRect.minY = rect.bottom.toDouble()
            rRect.maxX = rect.right.toDouble()
            rRect.maxY = rect.top.toDouble()
        }

        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.rects)))
        item.changeType = UpdatableChangeType.user.name
    }

    private fun rects(rects: List<RectF>, itemRects: List<RRect>): Boolean {
        if (rects.size != itemRects.size) {
            return true
        }

        for (rect in rects) {
            if (itemRects.firstOrNull {
                    it.minX == rect.left.toDouble()
                            && it.minY == rect.bottom.toDouble()
                            && it.maxX == rect.right.toDouble()
                            && it.maxY == rect.top.toDouble()
                } == null) {
                return true
            }
        }

        return false
    }
}