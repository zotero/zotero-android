package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class RUser : RealmObject() {
    @PrimaryKey
    var identifier: Int = 0
    var name: String = ""
    var username: String = ""

    @LinkingObjects("createdBy")
    val createdBy: RealmResults<RItem> = TODO()

    @LinkingObjects("lastModifiedBy")
    val modifiedBy: RealmResults<RItem> = TODO()
}
