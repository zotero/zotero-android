package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCustomLibrary

class ReadAllCustomLibrariesDbRequest : DbResponseRequest<RealmResults<RCustomLibrary>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RCustomLibrary> {
        return database.where<RCustomLibrary>().sort("orderId").findAll()
    }
}