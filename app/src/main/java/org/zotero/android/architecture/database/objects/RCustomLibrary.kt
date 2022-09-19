package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

enum class RCustomLibraryType(val libraryName: String) {
    myLibrary("My Library");

}
open class RCustomLibrary : RealmObject() {
    @PrimaryKey
    var type = RCustomLibraryType.myLibrary.name
    var orderId: Int = 0
    var versions: RVersions? = null
}
