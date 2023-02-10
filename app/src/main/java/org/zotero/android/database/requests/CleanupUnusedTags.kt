package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RTag

class CleanupUnusedTags :DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val toRemoveBase = database.where<RTag>().isEmpty("tags").and().equalTo("color", "").findAll()
        toRemoveBase.deleteAllFromRealm()
    }
}