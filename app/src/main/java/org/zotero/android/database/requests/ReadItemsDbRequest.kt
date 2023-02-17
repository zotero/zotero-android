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

class ReadItemsDbRequest(
    val libraryId: LibraryIdentifier,
    val collectionId: CollectionIdentifier,
    val defaults: Defaults
) : DbResponseRequest<RealmResults<RItem>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(
        database: Realm,
    ): RealmResults<RItem> {
        if (defaults.showSubcollectionItems() && collectionId is CollectionIdentifier.collection) {
            val keys = selfAndSubcollectionKeys(collectionId.key, database)
            return database
                .where<RItem>()
                .items(forCollectionsKeys = keys, libraryId = this.libraryId)
                .findAll()
        }
        return database
            .where<RItem>()
            .items(collectionId, libraryId = libraryId)
            .findAll()

    }

    private fun selfAndSubcollectionKeys(
        key: String,
        database: Realm
    ): Set<String> {
        var keys: Set<String> = hashSetOf(key)
        val children = database
            .where<RCollection>()
            .parentKey(key, this.libraryId)
            .findAll()
        for (child in children) {
            keys = keys.union(selfAndSubcollectionKeys(child.key, database))
        }
        return keys
    }
}

class ReadItemsWithKeysDbRequest(
    val keys: Set<String>,
    val libraryId: LibraryIdentifier,
) : DbResponseRequest<RealmResults<RItem>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RItem> {
        return database.where<RItem>().keys(this.keys, this.libraryId).findAll()
    }

}