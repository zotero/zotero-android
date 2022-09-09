package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey

class RUser: RealmObject() {
    @PrimaryKey
    var identifier: Int = 0
    var name: String = ""
    var username: String = ""

    @LinkingObjects("createdBy")
    lateinit var createdBy: RealmResults<RItem>

    @LinkingObjects("lastModifiedBy")
    lateinit var modifiedBy: RealmResults<RItem>
}
