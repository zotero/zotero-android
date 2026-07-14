package org.zotero.android.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.ktx.rounded
import java.util.Date

enum class RPageIndexChanges {
    index
}

open class RPageIndex : RealmObject(), Updatable, Syncable, Deletable {
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
    override var deleted: Boolean = false

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

            return mapOf(
                SettingKeyParser.uid(
                    key,
                    libraryId = libraryId,
                    prefix = "lastPageIndex"
                ) to mapOf("value" to value)
            )
        }

    override fun markAsChanged(database: Realm) {
        this.changes.add(RObjectChange.create(changes = listOf(RPageIndexChanges.index)))
        this.changeType = UpdatableChangeType.user.name
        this.deleted = false
        this.version = 0
    }

    override fun willRemove(database: Realm) {
        if (changes.isValid) {
            changes.deleteAllFromRealm()
        }
    }

    override val isInvalidated: Boolean
        get() = !isValid
}
