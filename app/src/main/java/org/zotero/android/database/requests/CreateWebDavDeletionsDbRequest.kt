package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RWebDavDeletion
import org.zotero.android.sync.LibraryIdentifier

class CreateWebDavDeletionsDbRequest(
    private val keys: List<String>,
    private val libraryId: LibraryIdentifier,
): DbResponseRequest<Boolean> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): Boolean {
        var didCreateDeletion = false
        val items = database.where<RItem>().keys(this.keys, this.libraryId).findAll()

        for (item in items) {
            if (item.rawType == ItemTypes.attachment) {
                didCreateDeletion = createDeletionIfNeeded(item.key, database = database) || didCreateDeletion
            } else {
                val items = item.children!!.where().item(type = ItemTypes.attachment).findAll()
                for (item in items) {
                    didCreateDeletion = createDeletionIfNeeded(item.key, database = database) || didCreateDeletion
                }
            }
        }

        return didCreateDeletion
    }

    private fun createDeletionIfNeeded(key: String, database: Realm): Boolean {
        if(database.where<RWebDavDeletion>().key(key, this.libraryId).findFirst() != null) {
            return false
        }
        val deletion = database.createObject<RWebDavDeletion>()
        deletion.key = key
        deletion.libraryId = this.libraryId
        return true
    }
}