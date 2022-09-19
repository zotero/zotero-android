package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

enum class ObjectSyncState {
    synced, dirty, outdated
}

interface Syncable {
    var key: String
    var customLibraryKey: String?
    var groupKey: Int?
    var version: Int
    var syncState: String
    var lastSyncDate: Date
    var syncRetries: Int
    fun isValid(): Boolean
    val isInvalidated: Boolean
        get() {
            return !isValid()
        }

    var libraryId: LibraryIdentifier?
        get() {
            if (isInvalidated) {
                return null
            }
            val keyCustom = this.customLibraryKey
            if (keyCustom != null) {
                return LibraryIdentifier.custom(RCustomLibraryType.valueOf(keyCustom))
            }
            val key = this.groupKey
            if (key != null) {
                return LibraryIdentifier.group(key)
            }

            return null
        }
        set(newValue) {
            val identifier = newValue
            if (identifier == null) {
                groupKey = null
                customLibraryKey = null
                return
            }

            when (identifier) {
                is LibraryIdentifier.custom ->
                    customLibraryKey = identifier.type.name
                is LibraryIdentifier.group ->
                    groupKey = identifier.groupId
            }
        }
}
