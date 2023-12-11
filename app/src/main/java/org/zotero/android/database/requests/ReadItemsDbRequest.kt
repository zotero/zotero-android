package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmQuery
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
    val defaults: Defaults,
    val isAsync: Boolean,
) : DbResponseRequest<RealmResults<RItem>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(
        database: Realm,
    ): RealmResults<RItem> {
        var resultsQuery: RealmQuery<RItem>
        if (defaults.showSubcollectionItems() && collectionId is CollectionIdentifier.collection) {
            val keys = selfAndSubcollectionKeys(collectionId.key, database)

            resultsQuery = database
                .where<RItem>()
                .items(forCollectionsKeys = keys, libraryId = this.libraryId)

        } else {
            resultsQuery = database
            .where<RItem>()
            .items(this.collectionId, libraryId = this.libraryId)
        }
        if (!this.searchTextComponents.isEmpty()) {
            resultsQuery = resultsQuery.itemSearch(this.searchTextComponents)
        }

        if (!this.filters.isEmpty()) {
            for (filter in this.filters) {
                when (filter) {
                    is ItemsFilter.downloadedFiles -> {
                        resultsQuery = resultsQuery.rawPredicate("fileDownloaded = true or any children.fileDownloaded = true")
                    }
                    is ItemsFilter.tags -> {
                        val tags = filter.tags
                        var predicates = resultsQuery
                        for (tag in tags) {
                            predicates = predicates.rawPredicate("any tags.tag.name == $0 or any children.tags.tag.name == $1 or SUBQUERY(children, \$item, any \$item.children.tags.tag.name == $2).@count > 0", tag, tag, tag)
                        }
                        resultsQuery = predicates
                    }
                }
            }
        }

        // Sort if needed
        if (this.sortType != null) {
            resultsQuery = resultsQuery.sort(
                this.sortType.descriptors.first,
                this.sortType.descriptors.second
            )
        }
        if (isAsync) {
            return resultsQuery.findAllAsync()
        } else {
            return resultsQuery.findAll()
        }
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