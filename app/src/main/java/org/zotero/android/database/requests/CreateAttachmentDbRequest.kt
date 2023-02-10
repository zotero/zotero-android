package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.TagResponse
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.formatter.iso8601DateFormatV2
import org.zotero.android.sync.LinkMode
import timber.log.Timber

class CreateAttachmentDbRequest(
    val attachment: Attachment,
    val parentKey: String?,
    val localizedType: String,
    val includeAccessDate: Boolean,
    val collections: Set<String>,
    val tags: List<TagResponse>,
    val fileStore: FileStore
) : DbResponseRequest<RItem> {

    sealed class Error : Exception() {
        object cantCreateMd5 : Error()
        object incorrectMd5Value : Error()
        object alreadyExists : Error()
    }

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RItem {
        if(database.where<RItem>().key(this.attachment.key, this.attachment.libraryId).findFirst() != null) {
            Timber.e("CreateAttachmentDbRequest: Trying to create attachment that already exists!")
            throw Error.alreadyExists
        }
        val changes = mutableListOf(
            RItemChanges.type,
            RItemChanges.fields,
            RItemChanges.tags
        )
        var item = database.createObject<RItem>()
        item.key = this.attachment.key
        item.rawType = ItemTypes.attachment
        item.localizedType = this.localizedType
        item.syncState = ObjectSyncState.synced.name
        item.set(title = this.attachment.title)
        item.changeType = UpdatableChangeType.user.name
        item.attachmentNeedsSync = true
        item.fileDownloaded = true
        item.dateAdded = this.attachment.dateAdded
        item.dateModified = this.attachment.dateAdded
        item.libraryId = this.attachment.libraryId

        val attachmentKeys = FieldKeys.Item.Attachment.fieldKeys

        for (fieldKey in attachmentKeys) {
            val value: String

            when (fieldKey) {
                FieldKeys.Item.title ->
                    value = this.attachment.title
                FieldKeys.Item.Attachment.linkMode -> {
                    value = when (this.attachment.type) {
                        is Attachment.Kind.file -> {
                            when (this.attachment.type.linkType) {
                                Attachment.FileLinkType.embeddedImage -> LinkMode.embeddedImage.str
                                Attachment.FileLinkType.importedFile -> LinkMode.importedFile.str
                                Attachment.FileLinkType.importedUrl -> LinkMode.importedUrl.str
                                Attachment.FileLinkType.linkedFile -> LinkMode.linkedFile.str
                            }
                        }
                        is Attachment.Kind.url -> LinkMode.linkedUrl.str
                    }
                }
                FieldKeys.Item.Attachment.contentType -> {
                    when (this.attachment.type) {
                        is Attachment.Kind.file -> value = this.attachment.type.contentType
                        is Attachment.Kind.url -> continue
                    }
                }
                FieldKeys.Item.Attachment.md5 -> {
                    when (this.attachment.type) {
                        is Attachment.Kind.file -> {
                            val file = fileStore.attachmentFile(
                                this.attachment.libraryId,
                                key = this.attachment.key,
                                filename = this.attachment.type.filename,
                                contentType = this.attachment.type.contentType
                            )
                            val md5Value = fileStore.md5(file)
                            if (md5Value == "<null>") {
                                Timber.e("CreateAttachmentDbRequest: incorrect md5 value " +
                                        "for attachment ${this.attachment.key}")
                                throw Error.incorrectMd5Value
                            }
                            value = md5Value
                        }

                        is Attachment.Kind.url -> {
                            continue
                        }
                    }
                }

                FieldKeys.Item.Attachment.mtime -> {
                    when (this.attachment.type) {
                        is Attachment.Kind.file -> {
                            val modificationTime = System.currentTimeMillis()
                            value = modificationTime.toString()
                        }
                        is Attachment.Kind.url -> {
                            continue
                        }
                    }
                }

                FieldKeys.Item.Attachment.filename -> {
                    when (this.attachment.type) {
                        is Attachment.Kind.file -> value =
                            this.attachment.type.filename
                        is Attachment.Kind.url -> continue
                    }
                }

                FieldKeys.Item.Attachment.url -> {
                    value = when (this.attachment.type) {
                        is Attachment.Kind.url -> this.attachment.type.url
                        else -> {
                            val url = this.attachment.url ?: continue
                            url
                        }
                    }
                }

                FieldKeys.Item.Attachment.path -> {
                    when {
                        this.attachment.type is Attachment.Kind.file &&
                                this.attachment.type.linkType == Attachment.FileLinkType.linkedFile -> {
                            val file = fileStore.attachmentFile(
                                this.attachment.libraryId,
                                key = this.attachment.key,
                                filename = this.attachment.type.filename,
                                contentType = this.attachment.type.contentType
                            )
                            value = file.absolutePath
                        }
                        else -> continue
                    }
                }
                FieldKeys.Item.accessDate -> {
                    if(!this.includeAccessDate) { continue }
                    value = iso8601DateFormatV2.format(this.attachment.dateAdded)
                }
                else -> continue
            }
            val field = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            field.key = fieldKey
            field.baseKey = null
            field.value = value
            field.changed = true
        }

        this.collections.forEach { key ->
            val collection: RCollection
            val existing =
                database
                    .where<RCollection>()
                    .key(key, this.attachment.libraryId)
                    .findFirst()
            if (existing != null) {
                collection = existing
            } else {
                collection = database.createObject<RCollection>()
                collection.key = key
                collection.syncState = ObjectSyncState.dirty.name
                collection.libraryId = this.attachment.libraryId
            }

            collection.items.add(item)
        }

        if (this.collections.isNotEmpty()) {
            changes.add(RItemChanges.collections)
        }

        val key = this.parentKey
        if (key != null) {
            val parent = database
                .where<RItem>()
                .key(key, this.attachment.libraryId)
                .findFirst()
            if (parent != null) {
                item.parent = parent
                changes.add(RItemChanges.parent)
            }
        }
        item.changes.add(RObjectChange.create(changes = changes))
        return item
    }

}