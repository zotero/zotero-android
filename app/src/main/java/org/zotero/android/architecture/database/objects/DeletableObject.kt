package org.zotero.android.architecture.database.objects//

import io.realm.Realm


interface _DeletableObject : Deletable

typealias DeletableObject = _DeletableObject

interface Deletable {
    var deleted: Boolean

    fun willRemove(database: Realm)
}
