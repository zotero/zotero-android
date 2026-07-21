package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmModel
import org.zotero.android.database.DbResponseRequest
import kotlin.reflect.KClass

class CountObjectsDbRequest(
    var clazz: KClass<out RealmModel>,
) : DbResponseRequest<Long> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Long {
        return database.where(clazz.java).count()
    }
}