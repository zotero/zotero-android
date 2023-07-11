package org.zotero.android.sync

import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RSearch
import org.zotero.android.uicomponents.Drawables

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

    val iconName: Int
        get() {
            return when (this.identifier) {
                is CollectionIdentifier.collection -> Drawables.cell_collection
                is CollectionIdentifier.custom -> {
                    return when (this.identifier.type) {
                        CollectionIdentifier.CustomType.all,
                        CollectionIdentifier.CustomType.publications -> Drawables.cell_document
                        CollectionIdentifier.CustomType.trash -> Drawables.cell_trash
                        CollectionIdentifier.CustomType.unfiled -> Drawables.cell_unfiled
                    }
                }
                is CollectionIdentifier.search -> Drawables.cell_document
            }
        }
    val isCollection: Boolean get() {
        return when (this.identifier) {
            is CollectionIdentifier.collection ->
                true
            is CollectionIdentifier.custom, is CollectionIdentifier.search ->
                false
        }
    }
}

