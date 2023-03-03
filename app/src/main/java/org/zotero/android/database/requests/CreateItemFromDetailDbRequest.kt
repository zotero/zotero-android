package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCreator
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.files.FileStore
import org.zotero.android.screens.itemdetails.data.ItemDetailData
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import timber.log.Timber

class CreateItemFromDetailDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val collectionKey: String?,
    private val data: ItemDetailData,
    private val attachments: List<Attachment>,
    private val notes: List<Note>,
    private val tags: List<Tag>,
    private val fileStore: FileStore,
    private val schemaController: SchemaController,
): DbResponseRequest<RItem> {
    sealed class Error : Exception() {
        object alreadyExists : Error()
    }

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RItem {
        if (database.where<RItem>().key(this.key, this.libraryId).findFirst() != null) {
            Timber.e("CreateItemFromDetailDbRequest: Trying to create item that already exists!")
            throw Error.alreadyExists
        }
        val item = database.createObject<RItem>()
        item.key = this.key
        item.rawType = this.data.type
        item.localizedType =
            this.schemaController.localizedItemType(itemType = this.data.type) ?: ""
        item.syncState = ObjectSyncState.synced.name
        item.dateAdded = this.data.dateAdded
        item.dateModified = this.data.dateModified
        item.libraryId = this.libraryId

        val changes = mutableListOf<RItemChanges>(RItemChanges.type, RItemChanges.fields)

        val collectionKey = this.collectionKey
        if (collectionKey != null) {
            val collection =
                database.where<RCollection>().key(collectionKey, this.libraryId).findFirst()
            if (collection != null) {
                collection.items.add(item)
                changes.add(RItemChanges.collections)
            }
        }

        for ((offset, creatorId) in this.data.creatorIds.withIndex()) {
            val creator = this.data.creators[creatorId] ?: continue

            val rCreator = database.createEmbeddedObject(RCreator::class.java, item, "creators")
            rCreator.rawType = creator.type
            rCreator.firstName = creator.firstName
            rCreator.lastName = creator.lastName
            rCreator.name = creator.name
            rCreator.orderId = offset
            rCreator.primary = creator.primary
        }
        item.updateCreatorSummary()

        if (!this.data.creators.isEmpty()) {
            changes.add(RItemChanges.creators)
        }

        for (field in this.data.databaseFields(schemaController = this.schemaController)) {
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseField
            rField.value = field.value
            rField.changed = true

            if (field.key == FieldKeys.Item.title || field.baseField == FieldKeys.Item.title) {
                item.baseTitle = field.value
            } else if (field.key == FieldKeys.Item.date) {
                //TODO setDateFieldMetadata
            } else if (field.key == FieldKeys.Item.publisher || field.baseField == FieldKeys.Item.publisher) {
                item.setP(publisher = field.value)
            } else if (field.key == FieldKeys.Item.publicationTitle || field.baseField == FieldKeys.Item.publicationTitle) {
                item.setPT(publicationTitle = field.value)
            }
        }

        for (note in this.notes) {
            val rNote = CreateNoteDbRequest(
                note = note,
                localizedType = (this.schemaController.localizedItemType(itemType = ItemTypes.note)
                    ?: ""),
                libraryId = this.libraryId,
                collectionKey = null,
                parentKey = null
            ).process(database)
            rNote.parent = item
            rNote.changes.add(RObjectChange.create(changes = listOf(RItemChanges.parent)))
        }

        for (attachment in this.attachments) {
            val rAttachment =
                database.where<RItem>().key(attachment.key, this.libraryId).findFirst()
            if (rAttachment != null) {
                rAttachment.parent = item
                rAttachment.changes.add(RObjectChange.create(changes = listOf(RItemChanges.parent)))
                rAttachment.changeType = UpdatableChangeType.user.name
            } else {
                val rAttachment = CreateAttachmentDbRequest(
                    fileStore = this.fileStore,
                    attachment = attachment,
                    parentKey = null,
                    localizedType = (this.schemaController.localizedItemType(itemType = ItemTypes.attachment)
                        ?: ""),
                    includeAccessDate = attachment.hasUrl,
                    collections = emptySet(), tags = emptyList()
                ).process(database)
                rAttachment.libraryId = this.libraryId
                rAttachment.parent = item
                rAttachment.changes.add(RObjectChange.create(changes = listOf(RItemChanges.parent)))
            }
        }

        for (tag in this.tags) {
            val rTag = database.where<RTag>().name(tag.name, this.libraryId).findFirst() ?: continue

            val rTypedTag = database.createObject<RTypedTag>()
            rTypedTag.type = RTypedTag.Kind.manual.name
            rTypedTag.item = item
            rTypedTag.tag = rTag
        }
        if (!this.tags.isEmpty()) {
            changes.add(RItemChanges.tags)
        }

        item.updateDerivedTitles()
        item.changes.add(RObjectChange.create(changes = changes))
        item.changeType = UpdatableChangeType.user.name

        return item

    }
}