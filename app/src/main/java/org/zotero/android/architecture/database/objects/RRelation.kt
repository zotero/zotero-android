package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class RRelation: RealmObject() {
    var type: String = ""
    var urlString: String = ""
}
