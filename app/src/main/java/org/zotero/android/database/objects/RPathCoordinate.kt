package org.zotero.android.database.objects

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class RPath : RealmObject() {
    var sortIndex: Int = 0
    val coordinates: RealmList<RPathCoordinate> = RealmList()
}

@RealmClass(embedded = true)
open class RPathCoordinate : RealmObject() {
    var value: Double = 0.0
    var sortIndex: Int = 0
}
