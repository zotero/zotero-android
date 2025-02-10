package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber
import java.util.Date

class LinkAttachmentToParentItemDbRequest(
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
    private val libraryId: LibraryIdentifier,
    private val itemKey: String,
    private val parentItemKey: String
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database
            .where<RItem>()
            .key(this.itemKey, this.libraryId)
            .findFirst()!!
        val parentItem = database
            .where<RItem>()
            .key(this.parentItemKey, this.libraryId)
            .findFirst()!!

        item.parent = parentItem

        for (collection in item.collections!!
            .where()
            .findAll()) {
            val index = collection.items.indexOf(item)
            if (index == -1) {
                continue
            }
            collection.items.removeAt(index)
        }

        val key = this.schemaController.titleKey(item.rawType)
        if (key == null) {
            Timber.e("LinkAttachmentToParentItemDbRequest: schema controller doesn't contain title key for item type ${item.rawType}")
            return
        }

        val keyPair = KeyBaseKeyPair(
            key = key,
            baseKey = (if (key != FieldKeys.Item.title) FieldKeys.Item.title else null)
        )

        EditItemFieldsDbRequest(
            key = itemKey,
            libraryId = libraryId,
            fieldValues = mapOf(keyPair to "PDF"),
            dateParser = dateParser
        ).process(database)

        item.changes.add(
            RObjectChange.create(
                changes = listOf(
                    RItemChanges.collections,
                    RItemChanges.parent,
                    RItemChanges.fields
                )
            )
        )
        item.changeType = UpdatableChangeType.user.name
        item.dateModified = Date()
    }
}