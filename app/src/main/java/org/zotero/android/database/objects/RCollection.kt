package org.zotero.android.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.kotlin.where
import org.zotero.android.database.requests.key
import org.zotero.android.database.requests.parentKey
import timber.log.Timber
import java.util.Date

enum class RCollectionChanges {
    nameS, parent
}


open class RCollection : Syncable, Updatable, Deletable, RealmObject() {

    @Index
    override var key: String = ""
    var name: String = ""
    lateinit var dateModified: Date
    var parentKey: String? = null
    var collapsed: Boolean = true
    lateinit var lastUsed: Date

    var items: RealmList<RItem> = RealmList()
    override var customLibraryKey: String? = null
    override var groupKey: Int? = null

    @Index
    override var version: Int = 0
    override lateinit var syncState: String//ObjectSyncState
    override var lastSyncDate: Date? = null
    override var syncRetries: Int = 0
    override val isInvalidated: Boolean
        get() = !isValid
    override lateinit var changes: RealmList<RObjectChange>
    override lateinit var changeType: String // UpdatableChangeType
    override var deleted: Boolean = false
    override fun willRemove(database: Realm) {
        val libraryId = this.libraryId
        if (libraryId != null) {
            val children = database.where<RCollection>().parentKey(this.key, library = libraryId)
            if (children.isValid) {
                for (child in children.findAll()) {
                    if (child.isInvalidated) {
                        continue
                    }
                    child.willRemove(database)
                }
                children.findAll().deleteAllFromRealm()
            }
        }
    }

    var trash: Boolean = false

    val changedFields: List<RCollectionChanges>
        get() {
            return changes.flatMap { it.rawChanges.map { RCollectionChanges.valueOf(it) } }
        }

    fun level(database: Realm): Int {
        val libraryId = this.libraryId
        if (libraryId == null) {
            return 0
        }

        val keys: MutableSet<String> = mutableSetOf(this.key)
        var level = 0
        var objectS: RCollection? = this

        while (objectS?.parentKey != null) {
            val parentKey = objectS.parentKey!!
            if (keys.contains(parentKey)) {
                Timber.i("RCollection: parent infinite loop; key=${parentKey}; keys=${keys}")
                return level
            }
            objectS = database.where<RCollection>().key(parentKey).findFirst()
            level += 1
            keys.add(parentKey)
        }

        return level
    }

    override val updateParameters: Map<String, Any>?
        get() {
            if (!isChanged) {
                return null
            }

            val parameters: MutableMap<String, Any> = mutableMapOf(
                "key" to this.key,
                "version" to this.version
            )

            val changes = this.changedFields
            if (changes.contains(RCollectionChanges.nameS)) {
                parameters["name"] = this.name
            }
            if (changes.contains(RCollectionChanges.parent)) {
                val key = this.parentKey
                if (key != null) {
                    parameters["parentCollection"] = key
                } else {
                    parameters["parentCollection"] = false
                }
            }

            return parameters
        }

    override val selfOrChildChanged: Boolean
        get() = isChanged

    override fun markAsChanged(database: Realm) {
        val changes = mutableListOf(RCollectionChanges.nameS)
        changeType = UpdatableChangeType.user.name
        deleted = false
        version = 0

        if (this.parentKey != null) {
            changes += RCollectionChanges.parent
        }

        this.changes.add(RObjectChange.create(changes = changes))

        this.items.forEach { item ->
            item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.collections)))
            item.changeType = UpdatableChangeType.user.name
        }

        val libraryId = this.libraryId
        if (libraryId != null) {
            val children = database.where<RCollection>().parentKey(key, libraryId).findAll()
            children.forEach { child ->
                child.markAsChanged(database)
            }
        }

    }

    companion object {
        val observableKeypathsForList = listOf("name", "parentKey", "items")
    }
}
