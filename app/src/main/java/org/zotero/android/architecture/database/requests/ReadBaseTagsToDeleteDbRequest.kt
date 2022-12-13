package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmCollection
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RTypedTag

open class ReadBaseTagsToDeleteDbRequest(val fromTags: RealmCollection<RTypedTag>) :
    DbResponseRequest<List<String>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): List<String> {
        return this.fromTags
            .where()
            .baseTagsToDelete()
            .findAll()
            .mapNotNull { it.tag?.name }
    }
}