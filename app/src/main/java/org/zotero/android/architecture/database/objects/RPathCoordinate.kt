package org.zotero.android.architecture.database.objects

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class RPath: RealmObject() {
    var sortIndex: Int = 0
    lateinit var coordinates: RealmList<RPathCoordinate>
}

@RealmClass(embedded = true)
open class RPathCoordinate: RealmObject() {
    var value: Double = 0.0
    var sortIndex: Int = 0
}
