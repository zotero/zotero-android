package org.zotero.android.architecture.database.objects//

import io.realm.Realm

interface Deletable {
    var deleted: Boolean

    fun willRemove(database: Realm)

    fun isValid(): Boolean
    val isInvalidated: Boolean
        get() {
            return !isValid()
        }

}
