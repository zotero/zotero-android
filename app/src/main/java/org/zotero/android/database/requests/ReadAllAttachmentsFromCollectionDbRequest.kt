package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

class ReadAllAttachmentsFromCollectionDbRequest(
    private val defaults: Defaults,
    private val collectionId: CollectionIdentifier,
    private val libraryId: LibraryIdentifier,
) : DbResponseRequest<RealmResults<RItem>> {
    override val needsWrite: Boolean
        get() = false

    sealed class Error : Exception() {
        object collectionIsTrash : Error()
    }


    override fun process(database: Realm): RealmResults<RItem> {
        val collectionIdLocal = this.collectionId
        if (collectionIdLocal.isTrash) {
            throw Error.collectionIsTrash
        }

        if (defaults.showSubcollectionItems()) {
            if (collectionIdLocal is CollectionIdentifier.collection) {
                val keys = selfAndSubcollectionKeys(collectionIdLocal.key, database)
                return database
                    .where<RItem>()
                    .allAttachments(keys, libraryId = this.libraryId)
                    .findAll()
            }
        }
        return database
            .where<RItem>()
            .allAttachments(this.collectionId, libraryId = this.libraryId)
            .findAll()
    }

    private fun selfAndSubcollectionKeys(key: String, database: Realm): Set<String> {
        var keys: Set<String> = setOf(key)
        val children = database.where<RCollection>().parentKey(key, this.libraryId).findAll()
        for (child in children) {
            keys = keys.union(selfAndSubcollectionKeys(key = child.key, database = database))
        }
        return keys
    }
}