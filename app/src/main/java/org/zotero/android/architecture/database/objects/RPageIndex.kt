package org.zotero.android.architecture.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

enum class RPageIndexChanges {
    index
}

open class RPageIndex : RealmObject(), Updatable, Syncable {
    @Index
    override var key: String = ""
    var index: Int = 0
    var changed: Boolean = false
    override var customLibraryKey: String? = null
    override var groupKey: Int? = null

    @Index
    override var version: Int = 0
    override lateinit var syncState: String
    override lateinit var lastSyncDate: Date
    override var syncRetries: Int = 0
    override lateinit var rawChangedFields: RealmList<String>
    override lateinit var changeType: String //UpdatableChangeType

    var changedFields: List<RPageIndexChanges>
        get() {
            return rawChangedFields.map { RPageIndexChanges.valueOf(it) }
        }

        set(newValue) {
            val z = RealmList<String>()
            z.addAll(newValue.map { it.name })
            rawChangedFields = z
        }


    override val updateParameters: Map<String, Any>?
        get() {
            val libraryId = this.libraryId
            if (libraryId == null) {
                return null
            }


            val libraryPart: String =
                when (libraryId) {
                    is LibraryIdentifier.custom ->
                        "u"
                    is LibraryIdentifier.group ->
                        "g${libraryId.groupId}"
                }

            return mapOf("lastPageIndex_${libraryPart}_${this.key}" to mapOf("value" to this.index))

        }

    override val selfOrChildChanged: Boolean
        get() = isChanged

    override fun markAsChanged(database: Realm) {
        //no-op
    }
}
