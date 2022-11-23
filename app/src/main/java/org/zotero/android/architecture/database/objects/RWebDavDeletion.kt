package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import org.zotero.android.sync.LibraryIdentifier

open class RWebDavDeletion : RealmObject() {
    lateinit var key: String
    var customLibraryKey: String? = null //RCustomLibraryType
    var groupKey: Int? = null

    var libraryId: LibraryIdentifier?
        get() {
            val key1 = this.customLibraryKey
            if (key1 != null) {
                return LibraryIdentifier.custom(RCustomLibraryType.valueOf(key1))
            }
            val key = this.groupKey
            if (key != null) {
                return LibraryIdentifier.group(key)
            }
            return null

        }
        set(identifier) {
            if (identifier == null) {
                this.groupKey = null
                this.customLibraryKey = null
                return
            }

            when (identifier) {
                is LibraryIdentifier.custom ->
                    this.customLibraryKey = identifier.type.name
                is LibraryIdentifier.group ->
                    this.groupKey = identifier.groupId
            }
        }
}
