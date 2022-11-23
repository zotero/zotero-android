package org.zotero.android.architecture.database.objects

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.RealmClass
import org.zotero.android.formatter.iso8601DateFormat
import java.util.Date

enum class RSearchChanges {
    nameS,
    conditions
}

open class RSearch : Deletable, Syncable, Updatable, RealmObject() {
    @Index
    override var key: String = ""
    var name: String = ""
    lateinit var dateModified: Date
    override var customLibraryKey: String? = null
    override var groupKey: Int? = null
    lateinit var conditions: RealmList<RCondition>

    @Index
    override var version: Int = 0
    override lateinit var syncState: String// ObjectSyncState
    override lateinit var lastSyncDate: Date
    override var syncRetries: Int = 0
    override lateinit var changes: RealmList<RObjectChange>
    override lateinit var changeType: String //UpdatableChangeType
    override var deleted: Boolean = false

    var trash: Boolean = false

    val changedFields: List<RSearchChanges>
        get() {
            return changes.flatMap { it.rawChanges.map { RSearchChanges.valueOf(it) } }
        }

    override fun willRemove(database: Realm) {
        //no-op
    }

    override val isInvalidated: Boolean
        get() = !isValid

    override val updateParameters: Map<String, Any>?
        get() {
            if (!isChanged) {
                return null
            }

            val parameters: MutableMap<String, Any> = mutableMapOf(
                "key" to this.key,
                "version" to this.version,
                "dateModified" to iso8601DateFormat.format(dateModified)
            )

            val changes = this.changedFields
            if (changes.contains(RSearchChanges.nameS)) {
                parameters["name"] = name
            }
            if (changes.contains(RSearchChanges.conditions)) {
                parameters["conditions"] = sortedConditionParameters
            }

            return parameters
        }

    private val sortedConditionParameters: List<Map<String, Any>>
        get() {
            return this.conditions.sortedBy { it.sortId }.map { it.updateParameters }
        }

    override val selfOrChildChanged: Boolean
        get() = isChanged

    override fun markAsChanged(database: Realm) {
        this.changes.add(RObjectChange.create(changes = RSearchChanges.values().toList()))
        changeType = UpdatableChangeType.user.name
        deleted = false
        version = 0
    }
}

@RealmClass(embedded = true)
open class RCondition(
    var condition: String = "",
    var operator: String = "",
    var value: String = "",
    var sortId: Int = 0
) : RealmObject() {

    val updateParameters: Map<String, Any>
        get() {
            return mapOf(
                "condition" to condition,
                "operator" to operator,
                "value" to value
            )
        }
}
