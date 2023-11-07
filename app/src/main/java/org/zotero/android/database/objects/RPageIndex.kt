package org.zotero.android.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

enum class RPageIndexChanges {
    index
}

open class RPageIndex : RealmObject(), Updatable, Syncable {
    @Index
    override var key: String = ""
    var index: String = ""
    var changed: Boolean = false
    override var customLibraryKey: String? = null
    override var groupKey: Int? = null

    @Index
    override var version: Int = 0
    override lateinit var syncState: String
    override var lastSyncDate: Date? = null
    override var syncRetries: Int = 0
    override lateinit var changes: RealmList<RObjectChange>
    override lateinit var changeType: String //UpdatableChangeType

    val changedFields: List<RPageIndexChanges>
        get() {
            return changes.flatMap {
                it.rawChanges.map { indexChanges ->
                    RPageIndexChanges.valueOf(
                        indexChanges
                    )
                }
            }
        }


    override val updateParameters: Map<String, Any>?
        get() {
            val libraryId = this.libraryId ?: return null

            val libraryPart: String =
                when (libraryId) {
                    is LibraryIdentifier.custom ->
                        "u"

                    is LibraryIdentifier.group ->
                        "g${libraryId.groupId}"
                }

            val value: Any
            val intValue = index.toIntOrNull()
            val doubleValue = index.toDoubleOrNull()
            if (intValue != null) {
                value = intValue
            } else if (doubleValue != null) {
                value = doubleValue.rounded(1)
            } else {
                value = index
            }

            return mapOf("lastPageIndex_${libraryPart}_${this.key}" to mapOf("value" to value))
        }

    override val selfOrChildChanged: Boolean
        get() = isChanged

    override fun markAsChanged(database: Realm) {
        //no-op
    }
}
