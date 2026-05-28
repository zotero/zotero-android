package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.files.FileStore
import org.zotero.android.sync.SchemaController

class CreateItemWithAttachmentDbRequest @AssistedInject constructor(
    @Assisted("item") private val item: ItemResponse,
    @Assisted("attachment") private val attachment: Attachment,

    private val schemaController: SchemaController,
    private val fileStore: FileStore,
    private val storeItemsDbResponseRequestFactory: StoreItemsDbResponseRequest.Factory
) : DbResponseRequest<Pair<RItem, RItem>> {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): Pair<RItem, RItem> {
        storeItemsDbResponseRequestFactory.create(
            responses = listOf(this.item),
            preferResponseData = true,
            denyIncorrectCreator = false
        )
            .process(database)
        val item = database.where<RItem>().key(this.item.key, this.attachment.libraryId).findFirst()

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

        val localizedType =
            this.schemaController.localizedItemType(itemType = ItemTypes.attachment) ?: ""
        val attachment = CreateAttachmentDbRequest(
            attachment = this.attachment,
            parentKey = null,
            localizedType = localizedType,
            includeAccessDate = this.attachment.hasUrl,
            collections = emptySet(),
            tags = emptyList(),
            fileStore = this.fileStore
        )
            .process(database)

        attachment.parent = item
        attachment.changes.add(RObjectChange.create(changes = listOf(RItemChanges.parent)))

        return item to attachment
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("item") item: ItemResponse,
            @Assisted("attachment") attachment: Attachment
        ): CreateItemWithAttachmentDbRequest
    }


}