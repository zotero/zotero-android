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
import org.zotero.android.sync.LibraryIdentifier

class EditAnnotationRectsDbRequestV2(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val rects: List<RectF>,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return
        if (!rectsChanged(this.rects, item.rects)) {
            return
        }
        sync(rects = this.rects, item = item, database = database)
    }

    private fun sync(rects: List<RectF>, item: RItem, database: Realm) {
        item.rects.deleteAllFromRealm()

        for (rect in rects) {
            val rRect = database.createEmbeddedObject(RRect::class.java, item, "rects")
            rRect.minX = rect.left.toDouble()
            rRect.maxX = rect.right.toDouble()
            rRect.minY = rect.top.toDouble()
            rRect.maxY = rect.bottom.toDouble()
        }

        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.rects)))
        item.changeType = UpdatableChangeType.user.name
    }

    private fun rectsChanged(rects: List<RectF>, itemRects: List<RRect>): Boolean {
        if (rects.size != itemRects.size) {
            return true
        }

        for (rect in rects) {
            if (itemRects.firstOrNull {
                    it.minX == rect.left.toDouble()
                            && it.minY == rect.top.toDouble()
                            && it.maxX == rect.right.toDouble()
                            && it.maxY == rect.bottom.toDouble()
                } == null) {
                return true
            }
        }

        return false
    }
}
