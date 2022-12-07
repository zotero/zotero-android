package org.zotero.android.architecture.database.objects

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.RealmClass
import java.util.UUID

@RealmClass(embedded = true)
open class RObjectChange : RealmObject() {
    lateinit var identifier: String
    var rawChanges: RealmList<String> = RealmList()

    companion object {
        //Result of this function must be added to _managed_ realm list
        fun <E : Enum<E>> create(changes: List<E>): RObjectChange {
            val objectChange = RObjectChange()
            objectChange.identifier = UUID.randomUUID().toString()
            val realmList = RealmList<String>()
            realmList.addAll(changes.map { it.name })
            objectChange.rawChanges = realmList
            return objectChange
        }
    }
}
