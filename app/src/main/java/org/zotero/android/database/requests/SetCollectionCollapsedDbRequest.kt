package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

class SetCollectionCollapsedDbRequest(
    private val collapsed: Boolean,
    private val identifier: CollectionIdentifier,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        when(this.identifier) {
            is CollectionIdentifier.collection -> {
                val collection = database.where<RCollection>().key(this.identifier.key,this.libraryId).findFirst() ?: return
                if (collection.collapsed == this.collapsed) {
                    return
                }
                collection.collapsed = collapsed
            }
            is CollectionIdentifier.search -> {
                //no-op
            }
            is CollectionIdentifier.custom -> {
                //no-op
            }
        }
    }
}