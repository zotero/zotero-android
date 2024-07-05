package org.zotero.android.database.requests

import io.realm.Realm
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RWebDavDeletion

class DeleteAllWebDavDeletionsDbRequest: DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        database.delete(RWebDavDeletion::class.java)
    }
}