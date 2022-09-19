package org.zotero.android.architecture.database.requests

import org.zotero.android.architecture.database.DbResponseRequest
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RPageIndex
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.sync.LibraryIdentifier
import kotlin.reflect.KClass

class ReadUpdatedSettingsUpdateParametersDbRequest(val libraryId: LibraryIdentifier):
    DbResponseRequest<List<Map<String, Any>>, List<Map<String, Any>>> {

    override val needsWrite: Boolean
        get() = false


    override fun process(
        database: Realm,
        clazz: KClass<List<Map<String, Any>>>?
    ): List<Map<String, Any>> {
        return when (this.libraryId) {
            is LibraryIdentifier.group ->
                listOf()
            is LibraryIdentifier.custom ->
                database.where<RPageIndex>().changed().findAll()
                    .mapNotNull { it.updateParameters }
        }
    }

}

private typealias ResponseType1 = List<Map<String, Any>>

class ReadUpdatedSearchUpdateParametersDbRequest(val libraryId: LibraryIdentifier):
    DbResponseRequest<ResponseType1, ResponseType1> {

    override val needsWrite: Boolean get() { return false }

    override fun process(database: Realm, clazz: KClass<ResponseType1>?): ResponseType1 {
        return database.where<RSearch>().changesWithoutDeletions(libraryId).findAll().mapNotNull { it.updateParameters }
    }

}

private typealias ResponseType2 = Pair<List<Map<String, Any>>,Boolean>

class ReadUpdatedItemUpdateParametersDbRequest(val libraryId: LibraryIdentifier):
    DbResponseRequest<ResponseType2, ResponseType2> {

    override val needsWrite: Boolean get() { return false }

    override fun process(database: Realm, clazz: KClass<ResponseType2>? ): ResponseType2 {
        val items = database.where<RItem>().itemChangesWithoutDeletions(libraryId).findAll()

        var hasUpload = false
        var keyToLevel = mutableMapOf<String, Int>()
        val levels = mutableMapOf<Int, MutableList<Map<String, Any>>>()
        for (item in items) {
            if (item.attachmentNeedsSync) {
                hasUpload = true
            }
            val parameters = item.updateParameters

            if (parameters == null) {
                continue
            }

            val level = level(item,  keyToLevel)
            keyToLevel[item.key] = level

            val array = levels[level]
            if (array != null) {
                array.add(parameters)
                levels[level] = array
            } else {
                levels[level] = mutableListOf(parameters)
            }
        }

        val results: MutableList<Map<String, Any>> = mutableListOf()
        levels.keys.sorted().forEach { level ->
            val parameters: MutableList<Map<String, Any>>? = levels[level]
                if (parameters != null){
                    parameters.forEach {
                        results.add(it)
                    }
                }
        }
        return results to hasUpload
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

private typealias ResponseType3 = List<Map<String, Any>>

class ReadUpdatedCollectionUpdateParametersDbRequest(val libraryId: LibraryIdentifier):
    DbResponseRequest<ResponseType3, ResponseType3> {

    override val needsWrite: Boolean get() { return false }

    override fun process(database: Realm, clazz: KClass<ResponseType3>?): ResponseType3 {
        val objects = database.where<RCollection>().changesWithoutDeletions(libraryId).findAll()

        if (objects.count() == 1) {
            return listOf(objects[0]?.updateParameters ?: emptyMap())
        }

        var levels: MutableMap<Int, MutableList<Map<String, Any>>> = mutableMapOf()

        for (objectS in objects) {
            val parameters = objectS.updateParameters
            if (parameters == null) {
                continue
            }
            val level = objectS.level(database)
            val array = levels[level]
            if (array != null) {
                array.add(parameters)
                levels[level] = array
            } else {
                levels[level] = mutableListOf(parameters)
            }
        }

        var results: MutableList<MutableMap<String, Any>> = mutableListOf()
        levels.keys.sorted().forEach { level ->
            val parameters = levels[level]
            if (parameters != null) {
                parameters.forEach {
                    results.add(it.toMutableMap())
                }
            }
        }
        return results
    }
}