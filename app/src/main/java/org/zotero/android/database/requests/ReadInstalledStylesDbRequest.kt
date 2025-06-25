package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle

class ReadInstalledStylesDbRequest : DbResponseRequest<RealmResults<RStyle>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RStyle> {
        return database.where<RStyle>().equalTo("installed", true).sort("title").findAll()
    }
}