package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmResults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RPageIndex
import kotlin.reflect.KClass

class ReadUserChangedObjectsDbRequest<T: RealmModel>(
    val clazz: KClass<T>
):DbResponseRequest<RealmResults<T>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<T> {
        if (clazz == RItem::class) {
            return database.where(clazz.java).itemUserChanges().findAll()
        } else if (clazz == RPageIndex::class) {
            return database.where(clazz.java).pageIndexUserChanges().findAll()
        } else {
            return database.where(clazz.java).userChanges().findAll()
        }

    }

}