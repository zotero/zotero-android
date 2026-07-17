package org.zotero.android.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.SettingKeyParser
import org.zotero.android.database.requests.key
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

enum class RLastReadDateChanges {
    date
}

open class RLastReadDate : Syncable, Updatable, Deletable, RealmObject() {
    @Index
    override var key: String = ""
    lateinit var date: Date
    var changed: Boolean = false
    override var groupKey: Int? = null
    override lateinit var changes: RealmList<RObjectChange>
    @Index
    override var version: Int = 0
    override lateinit var syncState: String//ObjectSyncState
    override var lastSyncDate: Date? = null
    override var syncRetries: Int = 0
    override val isInvalidated: Boolean
        get() = !isValid
    override var deleted: Boolean = false
    override lateinit var changeType: String // UpdatableChangeType

    override var customLibraryKey: String?
        get() = null
        set(value) {
            //no-op
        }

    override fun willRemove(database: Realm) {
        if (changes.isValid) {
            changes.deleteAllFromRealm()
        }
        if (groupKey == null) {
            return
        }

        val item = database.where<RItem>().key(key, LibraryIdentifier.group(groupKey!!)).findFirst()
            ?: return
        item.lastRead = null
        item.updateEffectiveLastRead()
    }

    override val updateParameters: Map<String, Any>?
        get() {
            if (groupKey == null) {
                return null
            }
            return mapOf(
                SettingKeyParser.uid(
                    key = key,
                    libraryId = LibraryIdentifier.group(groupKey!!),
                    prefix = "lastRead"
                ) to mapOf("value" to date.time / 1000)
            )
        }

    override fun markAsChanged(database: Realm) {
        changes.add(
            RObjectChange.create(
                changes = listOf(
                    RLastReadDateChanges.date
                )
            )
        )

        this.changeType = UpdatableChangeType.user.name
        this.deleted = false
        this.version = 0
    }

}