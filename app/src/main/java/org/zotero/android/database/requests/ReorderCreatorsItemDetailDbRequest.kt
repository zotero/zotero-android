package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.sync.LibraryIdentifier

class ReorderCreatorsItemDetailDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val ids: List<String>,

    ): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(key, libraryId).findFirst() ?: return
        for ((orderId, uuid) in ids.withIndex()) {
            item.creators.where().rawPredicate("uuid == %@", uuid).findFirst()?.orderId = orderId
        }
        item.updateCreatorSummary()
        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.creators)))
    }

}