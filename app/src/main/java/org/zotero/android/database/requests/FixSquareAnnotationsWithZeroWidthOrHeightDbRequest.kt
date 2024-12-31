package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem

class FixSquareAnnotationsWithZeroWidthOrHeightDbRequest : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val targetTypes = listOf(AnnotationType.image.name)
        val results = database.where<RItem>()
            .item(type = ItemTypes.annotation)
            .deleted(false)
            .`in`("annotationType", targetTypes.toTypedArray())
            .findAll()

        for (rItem in results) {
            val rect = rItem.rects.getOrNull(0) ?: continue
            val libraryId = rItem.libraryId ?: continue
            val width = rect.maxX - rect.minX
            val height = rect.maxY - rect.minY
            if (width == 0.0 || height == 0.0) {
                MarkObjectsAsDeletedDbRequest(
                    clazz = RItem::class,
                    keys = listOf(rItem.key),
                    libraryId = libraryId
                ).process(database)
            }
        }
    }
}