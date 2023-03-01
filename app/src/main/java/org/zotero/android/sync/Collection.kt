package org.zotero.android.sync

import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RSearch

data class Collection(
    val identifier: CollectionIdentifier,
    val name: String,
    val itemCount: Int
) {

    companion object {
        fun initWithCollection(objectS: RCollection, itemCount: Int): Collection {
            return Collection(
                identifier = CollectionIdentifier.collection(objectS.key),
                name = objectS.name,
                itemCount = itemCount
            )
        }

        fun initWithSearch(objectS: RSearch): Collection {
            return Collection(
                identifier = CollectionIdentifier.search(objectS.key),
                name = objectS.name,
                itemCount = 0,
            )
        }
        fun initWithCustomType(type: CollectionIdentifier.CustomType, itemCount: Int = 0): Collection {
            val name = when(type) {
                CollectionIdentifier.CustomType.all -> "All Items"
                CollectionIdentifier.CustomType.trash -> "Trash"
                CollectionIdentifier.CustomType.publications -> "My Publications"
                CollectionIdentifier.CustomType.unfiled -> "Unfiled Items"
            }
            return Collection(
                identifier = CollectionIdentifier.custom(type),
                itemCount = itemCount,
                name = name,
            )
        }
    }
}

