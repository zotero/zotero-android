package org.zotero.android.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

open class RUser : RealmObject() {
    @PrimaryKey
    var identifier: Long = 0
    var name: String = ""
    var username: String = ""

    @LinkingObjects("createdBy")
    val createdBy: RealmResults<RItem>? = null

    @LinkingObjects("lastModifiedBy")
    val modifiedBy: RealmResults<RItem>? = null
}
