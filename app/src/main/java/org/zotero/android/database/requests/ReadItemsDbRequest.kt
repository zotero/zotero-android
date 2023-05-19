package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

class ReadItemsDbRequest(
    val libraryId: LibraryIdentifier,
    val collectionId: CollectionIdentifier,
    val filters: List<ItemsFilter> = emptyList(),
    val sortType: ItemsSortType? = null,
    val searchTextComponents: List<String> = emptyList(),
    val defaults: Defaults
) : DbResponseRequest<RealmResults<RItem>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(
        database: Realm,
    ): RealmResults<RItem> {
        var results: RealmResults<RItem>
        if (defaults.showSubcollectionItems() && collectionId is CollectionIdentifier.collection) {
            val keys = selfAndSubcollectionKeys(collectionId.key, database)

            results = database
                .where<RItem>()
                .items(forCollectionsKeys = keys, libraryId = this.libraryId)
                .findAll()
        } else {
            results = database
            .where<RItem>()
            .items(this.collectionId, libraryId = this.libraryId)
            .findAll()
        }
        if (!this.searchTextComponents.isEmpty()) {
            results = results.where().itemSearch(this.searchTextComponents).findAll()
        }

        if (!this.filters.isEmpty()) {
            for (filter in this.filters) {
                when (filter) {
                    is ItemsFilter.downloadedFiles -> {
                        results = results.where().rawPredicate("fileDownloaded = true or any children.fileDownloaded = true").findAll()
                    }
                    is ItemsFilter.tags -> {
                        val tags = filter.tags
                        var predicates = results.where()
                        for (tag in tags) {
                            predicates = predicates.rawPredicate("any tags.tag.name == $0 or any children.tags.tag.name == $1 or SUBQUERY(children, \$item, any \$item.children.tags.tag.name == $2).@count > 0", tag, tag, tag)
                        }
                        results = predicates.findAll()
                    }
                }
            }
        }
        // Sort if needed
        return this.sortType?.let { sort ->
            results.sort(
                sort.descriptors.first,
                sort.descriptors.second
            )
        } ?: results
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