package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier

class PerformDeletionsDbRequest(
    val libraryId: LibraryIdentifier,
    val collections: List<String>,
    val items: List<String>,
    val searches: List<String>,
    val tags: List<String>,
    val conflictMode: ConflictResolutionMode,
) : DbResponseRequest<List<Pair<String, String>>> {
    enum class ConflictResolutionMode {
        resolveConflicts,
        deleteConflicts,
        restoreConflicts,
    }

    override val needsWrite: Boolean
        get() = true

    override fun process(
        database: Realm,
    ): List<Pair<String, String>> {
        deleteCollections(this.collections, database = database)
        deleteSearches(this.searches, database = database)
        val conflicts = this.deleteItems(this.items, database = database)
        this.deleteTags(this.tags, database = database)
        return conflicts
    }

    private fun deleteItems(keys: List<String>, database: Realm): List<Pair<String, String>> {
        val objects = database.where<RItem>().keys(keys, this.libraryId).findAll()
        val conflicts: MutableList<Pair<String, String>> = mutableListOf()
        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }
            when (this.conflictMode) {
                ConflictResolutionMode.resolveConflicts -> {
                    if (objectS.selfOrChildChanged) {
                        conflicts.add(Pair(objectS.key, objectS.displayTitle))
                        continue
                    }
                }
                ConflictResolutionMode.restoreConflicts -> {
                    if (objectS.selfOrChildChanged) {
                        objectS.markAsChanged(database)
                        continue
                    }
                }
                ConflictResolutionMode.deleteConflicts -> {
                    // no-op
                }
            }
            objectS.willRemove(database)
            objectS.deleteFromRealm()
        }
        return conflicts
    }

    private fun deleteCollections(keys: List<String>, database: Realm) {
        val objects = database
            .where<RCollection>()
            .keys(keys, this.libraryId)
            .findAll()

        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }

            if (objectS.isChanged) {
                objectS.markAsChanged(database)
            } else {
                objectS.willRemove(database)
                objectS.deleteFromRealm()
            }
        }
    }

    private fun deleteSearches(keys: List<String>, database: Realm) {
        val objects = database
            .where<RSearch>()
            .keys(keys, this.libraryId)
            .findAll()

        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }
            if (objectS.isChanged) {
                objectS.markAsChanged(database)
            } else {
                objectS.willRemove(database)
                objectS.deleteFromRealm()
            }
        }
    }

    private fun deleteTags(names: List<String>, database: Realm) {
        val tags = database
            .where<RTag>()
            .nameIn(names, this.libraryId)
            .findAll()
        for (tag in tags) {
            tag.tags?.deleteAllFromRealm()
        }
        tags.deleteAllFromRealm()
    }

}