package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class RCreator: RealmObject() {
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

    val updateParameters: Map<String, Any>
        get() {
            var parameters: MutableMap<String, Any> = mutableMapOf("creatorType" to this.rawType)
            if (!this.name.isEmpty()) {
                parameters["name"] = this.name
            } else if (!this.firstName.isEmpty() || !this.lastName.isEmpty()) {
                parameters["firstName"] = this.firstName
                parameters["lastName"] = this.lastName
            }
            return parameters
        }
}
