package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange

class CreateBackendItemDbRequest @AssistedInject constructor(
    @Assisted private val item: ItemResponse,

    private val storeItemsDbResponseRequestFactory: StoreItemsDbResponseRequest.Factory,
) : DbResponseRequest<RItem> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RItem {
        val libraryId = this.item.library.libraryId ?: throw DbError.objectNotFound

        storeItemsDbResponseRequestFactory.create(
            responses = listOf(this.item),
            preferResponseData = true,
            denyIncorrectCreator = false
        )
            .process(database)
        val item = database.where<RItem>().key(this.item.key, libraryId).findFirst()
        if (item == null) {
            throw DbError.objectNotFound
        }
        val changes = listOf(
            RItemChanges.type,
            RItemChanges.trash,
            RItemChanges.collections,
            RItemChanges.fields,
            RItemChanges.tags,
            RItemChanges.creators
        )
        item.changes.add(RObjectChange.create(changes = changes))
        item.fields.forEach { it.changed = true }

        return item

    }

    @AssistedFactory
    interface Factory {
        fun create(item: ItemResponse): CreateBackendItemDbRequest
    }

}