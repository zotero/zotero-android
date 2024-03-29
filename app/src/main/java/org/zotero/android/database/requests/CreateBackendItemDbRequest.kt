package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.SchemaController

class CreateBackendItemDbRequest(
    private val item: ItemResponse,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
) : DbResponseRequest<RItem> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RItem {
        val libraryId = this.item.library.libraryId ?: throw DbError.objectNotFound

        StoreItemsDbResponseRequest(
            responses = listOf(this.item),
            schemaController = this.schemaController,
            dateParser = this.dateParser,
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

}