package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

class EmptyTrashDbRequest(
    private val libraryId: LibraryIdentifier
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        database.where<RItem>().items(
            forCollectionId = CollectionIdentifier.custom(CollectionIdentifier.CustomType.trash),
            libraryId = this.libraryId
        ).findAll().forEach {
            it.deleted = true
            it.changeType = UpdatableChangeType.user.name
        }

    }
}