package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RGroup

class ReadGroupDbRequest(val identifier: Int) : DbResponseRequest<RGroup> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RGroup {
        val group = database.where<RGroup>().equalTo("identifier", this.identifier).findFirst()
        if (group == null) {
            throw DbError.objectNotFound
        }
        return group
    }
}