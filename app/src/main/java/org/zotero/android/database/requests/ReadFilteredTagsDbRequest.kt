package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag

class ReadFilteredTagsDbRequest(
    private val collectionId: CollectionIdentifier,
    private val libraryId: LibraryIdentifier,
    private val showAutomatic: Boolean,
    private val filters: List<ItemsFilter>

): DbResponseRequest<Set<Tag>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Set<Tag> {

        var predicates = database.where<RTypedTag>().typedTagLibrary(this.libraryId)

        val thisCollectionId = this.collectionId
        when(thisCollectionId) {
            is CollectionIdentifier.collection -> {
                predicates = predicates.and().rawPredicate("any item.collections.key = \"${thisCollectionId.key}\"")
            }
            is CollectionIdentifier.custom -> {
                when(thisCollectionId.type) {
                    CollectionIdentifier.CustomType.all, CollectionIdentifier.CustomType.publications -> {
                        //no-op
                    }
                    CollectionIdentifier.CustomType.unfiled -> {
                        predicates = predicates.and().rawPredicate("any item.collections.@count == 0")
                    }
                    CollectionIdentifier.CustomType.trash -> {
                        predicates = predicates.and().rawPredicate("item.trash = true")
                    }
                }
            }
            is CollectionIdentifier.search -> {
                //no-op
            }
        }
        if (!this.showAutomatic) {
            predicates = predicates.and().rawPredicate("tag.color != \"\" or type = \"${RTypedTag.Kind.manual.name}\"")
        }
        for (filter in this.filters) {
            when(filter) {
                ItemsFilter.downloadedFiles -> {
                    predicates = predicates.and().rawPredicate("item.fileDownloaded = true or any item.children.fileDownloaded = true")
                }
                is ItemsFilter.tags -> {
                    for (name in filter.tags) {
                        predicates = predicates.and().rawPredicate("any item.tags.tag.name == \"${name}\"")
                    }
                }
            }
        }
        val rTypedTags = predicates.findAll()
        val tags = mutableSetOf<Tag>()
        for (rTypedTag in rTypedTags) {
            if (rTypedTag.tag == null) continue
            val tag = Tag(rTypedTag.tag!!)
            tags.add(tag)
        }
        return tags

    }
}