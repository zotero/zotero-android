package org.zotero.android.architecture.database.objects//

import io.realm.Realm
import io.realm.RealmObject

interface Deletable {
    var deleted: Boolean

    fun willRemove(database: Realm)
}
