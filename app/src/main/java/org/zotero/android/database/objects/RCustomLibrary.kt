package org.zotero.android.database.objects

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class RCustomLibraryType : java.io.Serializable {
    myLibrary;

    val libraryName: String get() {
        when (this) {
            myLibrary ->
            return "My Library"
        }
    }
}

open class RCustomLibrary : RealmObject() {
    @PrimaryKey
    var type = RCustomLibraryType.myLibrary.name
    var orderId: Int = 0
    var versions: RVersions? = null
}
