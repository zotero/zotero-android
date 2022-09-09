package org.zotero.android.architecture.database.objects

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.zotero.android.sync.LibraryIdentifier

class RTypedTag : RealmObject() {
    enum class Kind(val q: Int) {
        automatic(1),
        manual(0)
    }

    var type = Kind.automatic.name
    var tag: RTag? = null
    var item: RItem? = null
}

class RTag : RealmObject() {
    @Index
    var name: String = ""
    val color: String = ""
    var customLibraryKey: RCustomLibraryType? = null
    var groupKey: Int? = null

    @LinkingObjects("tag")
    lateinit var tags: RealmList<RTypedTag>

    var libraryId: LibraryIdentifier?
        get() {
            if (customLibraryKey != null) {
                return LibraryIdentifier.custom(customLibraryKey!!)
            }
            if (groupKey != null) {
                return LibraryIdentifier.group(groupKey!!)
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
                    customLibraryKey = identifier.type
                is LibraryIdentifier.group ->
                    groupKey = identifier.groupId
            }
        }
}
