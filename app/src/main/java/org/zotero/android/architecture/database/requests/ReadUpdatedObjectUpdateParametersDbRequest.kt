package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RPageIndex
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

data class ReadUpdatedParametersResponse(
    val parameters: List<Map<String, Any>>,
    val changeUuids: Map<String, List<String>>
)

class ReadUpdatedSettingsUpdateParametersDbRequest(val libraryId: LibraryIdentifier) :
    DbResponseRequest<ReadUpdatedParametersResponse, ReadUpdatedParametersResponse> {

    override val needsWrite: Boolean
        get() = false


    override fun process(
        database: Realm,
        clazz: KClass<ReadUpdatedParametersResponse>?
    ): ReadUpdatedParametersResponse {
        return when (this.libraryId) {
            is LibraryIdentifier.group ->
                ReadUpdatedParametersResponse(emptyList(), emptyMap())
            is LibraryIdentifier.custom -> {
                val parameters = mutableListOf<Map<String, Any>>()
                val uuids = mutableMapOf<String, List<String>>()
                val changed = database
                    .where<RPageIndex>()
                    .changed()
                    .findAll()

                for (objectS in changed) {
                    val _parameters = objectS.updateParameters ?: continue
                    parameters.add(_parameters)
                    uuids[objectS.key] = objectS.changes.map { it.identifier }
                }

                return ReadUpdatedParametersResponse(parameters = parameters, changeUuids = uuids)
            }
        }
    }
}

class ReadUpdatedSearchUpdateParametersDbRequest(val libraryId: LibraryIdentifier) :
    DbResponseRequest<ReadUpdatedParametersResponse, ReadUpdatedParametersResponse> {

    override val needsWrite: Boolean
        get() {
            return false
        }

    override fun process(
        database: Realm,
        clazz: KClass<ReadUpdatedParametersResponse>?
    ): ReadUpdatedParametersResponse {
        val parameters = mutableListOf<Map<String, Any>>()
        val uuids = mutableMapOf<String, List<String>>()
        val changed = database
            .where<RSearch>()
            .changesWithoutDeletions(this.libraryId)
            .findAll()

        for (objectS in changed) {
            val _parameters = objectS.updateParameters ?: continue
            parameters.add(_parameters)
            uuids[objectS.key] = objectS.changes.map { it.identifier }
        }
        return ReadUpdatedParametersResponse(parameters = parameters, changeUuids = uuids)
    }
}

class ReadUpdatedItemUpdateParametersDbRequest(val libraryId: LibraryIdentifier) :
    DbResponseRequest<
            Pair<ReadUpdatedParametersResponse, Boolean>,
            Pair<ReadUpdatedParametersResponse, Boolean>> {

    override val needsWrite: Boolean
        get() {
            return false
        }

    override fun process(
        database: Realm,
        clazz: KClass<Pair<ReadUpdatedParametersResponse, Boolean>>?
    ): Pair<ReadUpdatedParametersResponse, Boolean> {
        val objects = database
            .where<RItem>()
            .itemChangesWithoutDeletions(this.libraryId)
            .findAll()

        if (objects.size == 1) {
            val item = objects.first()
            if (item != null) {
                val parameters = item.updateParameters
                if (parameters != null) {
                    val uuids = item.changes.map { it.identifier }
                    return ReadUpdatedParametersResponse(
                        parameters = listOf(parameters),
                        changeUuids = mapOf(item.key to uuids)
                    ) to item.attachmentNeedsSync
                }
            }
        }

        var hasUpload = false
        val keyToLevel = mutableMapOf<String, Int>()
        val levels = mutableMapOf<Int, MutableList<Map<String, Any>>>()
        val uuids = mutableMapOf<String, List<String>>()
        for (item in objects) {
            if (item.attachmentNeedsSync) {
                hasUpload = true
            }
            val parameters = item.updateParameters ?: continue

            val level = level(item, keyToLevel)
            keyToLevel[item.key] = level
            uuids[item.key] = item.changes.map { it.identifier }

            val array = levels[level]
            if (array != null) {
                array.add(parameters)
                levels[level] = array
            } else {
                levels[level] = mutableListOf(parameters)
            }
        }

        val results: MutableList<Map<String, Any>> = mutableListOf()
        for (level in levels.keys.sorted()) {
            val parameters = levels[level] ?: continue
            parameters.forEach {
                results.add(it)
            }
        }
        return (ReadUpdatedParametersResponse(
            parameters = results,
            changeUuids = uuids
        ) to hasUpload)
    }

    private fun level(item: RItem, levelCache: Map<String, Int>): Int {
        var level = 0
        var parent: RItem? = item.parent

        while (parent != null) {
            val currentLevel = levelCache[parent.key]
            if (currentLevel != null) {
                level += currentLevel + 1
                break
            }
            parent = parent.parent
            level += 1
        }

        return level
    }
}

class ReadUpdatedCollectionUpdateParametersDbRequest(val libraryId: LibraryIdentifier) :
    DbResponseRequest<ReadUpdatedParametersResponse, ReadUpdatedParametersResponse> {

    override val needsWrite: Boolean
        get() {
            return false
        }

    override fun process(
        database: Realm,
        clazz: KClass<ReadUpdatedParametersResponse>?
    ): ReadUpdatedParametersResponse {
        val objects = database
            .where<RCollection>()
            .changesWithoutDeletions(libraryId)
            .findAll()

        if (objects.size == 1) {
            val collection = objects.first()
            if (collection != null) {
                val parameters = collection.updateParameters
                if (parameters != null) {
                    val uuids = collection.changes.map { it.identifier }
                    return ReadUpdatedParametersResponse(
                        parameters = listOf(parameters),
                        changeUuids = mapOf(collection.key to uuids)
                    )
                }
            }
        }

        val levels: MutableMap<Int, MutableList<Map<String, Any>>> = mutableMapOf()
        val uuids = mutableMapOf<String, List<String>>()

        for (objectS in objects) {
            val parameters = objectS.updateParameters ?: continue

            uuids[objectS.key] = objectS.changes.map { it.identifier }

            val level = objectS.level(database)
            val array = levels[level]
            if (array != null) {
                array.add(parameters)
                levels[level] = array
            } else {
                levels[level] = mutableListOf(parameters)
            }
        }

        val results: MutableList<MutableMap<String, Any>> = mutableListOf()
        levels.keys.sorted().forEach { level ->
            val parameters = levels[level]
            parameters?.forEach {
                results.add(it.toMutableMap())
            }
        }
        return ReadUpdatedParametersResponse(parameters = results, changeUuids = uuids)
    }
}