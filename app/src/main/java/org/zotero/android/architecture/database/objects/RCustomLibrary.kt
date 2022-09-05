package org.zotero.android.architecture.database.objects

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

enum class RCustomLibraryType(val libraryName: String) {
    myLibrary("My Library");

}

class RCustomLibrary : RealmObject {
    @PrimaryKey
    var type = RCustomLibraryType.myLibrary.name
    var orderId: Int = 0
    var versions: RVersions? = null
}
