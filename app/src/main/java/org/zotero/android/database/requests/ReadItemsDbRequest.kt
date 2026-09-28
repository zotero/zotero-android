package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import io.realm.kotlin.where
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.CollectionIdentifier.CustomType
import org.zotero.android.sync.LibraryIdentifier

class ReadItemsDbRequest @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("collectionId") private val collectionId: CollectionIdentifier,
    @Assisted("filters") private val filters: List<ItemsFilter> = emptyList(),
    @Assisted("sortType") private val sortType: ItemsSortType? = null,
    @Assisted("searchTextComponents") private val searchTextComponents: List<String> = emptyList(),
    @Assisted("isAsync") private val isAsync: Boolean,

    val defaults: Defaults,
) : DbResponseRequest<RealmResults<RItem>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(
        database: Realm,
    ): RealmResults<RItem> {
        var resultsQuery: RealmQuery<RItem>
        if (defaults.showSubcollectionItems() && collectionId is CollectionIdentifier.collection) {
            val keys = database.selfAndSubcollectionKeys(collectionId.key, this.libraryId)

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
        val colIdLocal = collectionId
        if (colIdLocal is CollectionIdentifier.custom && colIdLocal.type == CustomType.recentlyRead) {
            resultsQuery = resultsQuery.sort("effectiveLastRead", Sort.DESCENDING, "sortTitle", Sort.ASCENDING)
        } else {
            if (this.sortType != null) {
                resultsQuery = resultsQuery.sort(
                    this.sortType.descriptors.first,
                    this.sortType.descriptors.second
                )
            }
        }

        return if (isAsync) {
            resultsQuery.findAllAsync()
        } else {
            resultsQuery.findAll()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("collectionId") collectionId: CollectionIdentifier,
            @Assisted("filters") filters: List<ItemsFilter> = emptyList(),
            @Assisted("sortType") sortType: ItemsSortType? = null,
            @Assisted("searchTextComponents") searchTextComponents: List<String> = emptyList(),
            @Assisted("isAsync") isAsync: Boolean,
        ): ReadItemsDbRequest
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