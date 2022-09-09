package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
class RCreator: RealmObject() {
    var rawType: String = ""
    var firstName: String= ""
    var lastName: String= ""
    var name: String= ""
    var orderId: Int = 0
    var primary: Boolean = false

    val summaryName: String get() {
        if (!name.isEmpty()) {
            return name
        }

        if (!lastName.isEmpty()) {
            return lastName
        }

        return firstName
    }
}
