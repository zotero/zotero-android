package org.zotero.android.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import org.zotero.android.sync.LibraryIdentifier

open class RTypedTag : RealmObject() {
    enum class Kind(val int: Int) {
        automatic(1),
        manual(0);

        companion object {
            private val map = values().associateBy(Kind::int)

            fun from(int: Int) = map[int]
        }
    }

    var type = Kind.automatic.name
    var tag: RTag? = null
    var item: RItem? = null
}

open class RTag : RealmObject() {
    @Index
    var name: String = ""
    var sortName: String =""
    var color: String = ""
    var order: Int = -1
    var customLibraryKey: String? = null // RCustomLibraryType
    var groupKey: Int? = null

    @LinkingObjects("tag")
    val tags: RealmResults<RTypedTag>? = null

    fun updateSortName() {
        val newName = RTag.sortName(this.name)
        if (newName != this.sortName) {
            this.sortName = newName
        }
    }

    companion object {
        fun sortName(name: String): String {
            return name.trim { "[]'\"".contains(it)}.lowercase()
        }
    }

    var libraryId: LibraryIdentifier?
        get() {
            if (customLibraryKey != null) {
                return LibraryIdentifier.custom(RCustomLibraryType.valueOf(customLibraryKey!!))
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
                    customLibraryKey = identifier.type.name
                is LibraryIdentifier.group ->
                    groupKey = identifier.groupId
            }
        }

    val isInvalidated: Boolean
        get() = !isValid
}
