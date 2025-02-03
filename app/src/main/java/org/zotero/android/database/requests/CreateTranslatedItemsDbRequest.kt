package org.zotero.android.database.requests

import io.realm.Realm
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.SchemaController

class CreateTranslatedItemsDbRequest(
    private val responses: List<ItemResponse>,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
) : DbResponseRequest<List<RItem>> {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): List<RItem> {
        val listOfCreatedItems: MutableList<RItem> = mutableListOf()
        for (response in this.responses) {
            val (item, _) = StoreItemDbRequest(
                response = response,
                schemaController = this.schemaController,
                dateParser = this.dateParser,
                preferRemoteData = true,
                denyIncorrectCreator = false,
            )
                .process(database)

            item.changeType = UpdatableChangeType.user.name
            for (field in item.fields) {
                field.changed = true
            }

            val changes: MutableList<RItemChanges> = mutableListOf(
                RItemChanges.type,
                RItemChanges.fields,
                RItemChanges.trash,
                RItemChanges.tags
            )
            if (!item.collections!!.isEmpty()) {
                changes.add(RItemChanges.collections)
            }
            if (!item.relations.isEmpty()) {
                changes.add(RItemChanges.relations)
            }
            if (!item.creators.isEmpty()) {
                changes.add(RItemChanges.creators)
            }
            if (!item.tags!!.isEmpty()) {
                changes.add(RItemChanges.tags)
            }
            item.changes.add(RObjectChange.create(changes = changes))
            listOfCreatedItems.add(item)
        }
        return listOfCreatedItems
    }
}