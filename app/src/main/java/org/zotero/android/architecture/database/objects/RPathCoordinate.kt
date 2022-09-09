package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
class RPath: RealmObject() {
    var sortIndex: Int = 0
    lateinit var coordinates: RealmResults<RPathCoordinate>
}

@RealmClass(embedded = true)
class RPathCoordinate: RealmObject() {
    var value: Double = 0.0
    var sortIndex: Int = 0
}
