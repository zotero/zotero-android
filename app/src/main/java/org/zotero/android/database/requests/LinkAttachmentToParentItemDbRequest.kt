package org.zotero.android.database.requests

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.AllItemsDbRowCreator
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber
import java.util.Date

class LinkAttachmentToParentItemDbRequest @AssistedInject constructor(
    private val schemaController: SchemaController,
    private val fileStore: FileStore,
    private val editItemFieldsDbRequestFactory: EditItemFieldsDbRequest.Factory,

    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("itemKey") private val itemKey: String,
    @Assisted("parentItemKey") private val parentItemKey: String
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

        val titleFieldKeyPair = KeyBaseKeyPair(
            key = key,
            baseKey = (if (key != FieldKeys.Item.title) FieldKeys.Item.title else null)
        )

        val oldFileName =
            item.fields.where().key(FieldKeys.Item.Attachment.filename).findFirst()!!.value
        val oldFile = fileStore.attachmentFile(libraryId, itemKey, oldFileName)

        val newFileName = getValidFileName("${getFileBaseNameFromItem(parentItem)}.pdf")
        val newFile = fileStore.attachmentFile(libraryId, itemKey, newFileName)

        oldFile.renameTo(newFile)

        val attachmentFileNamePair = KeyBaseKeyPair(
            key = FieldKeys.Item.Attachment.filename,
            baseKey = null
        )

        editItemFieldsDbRequestFactory.create(
            key = itemKey,
            libraryId = libraryId,
            fieldValues = mapOf(titleFieldKeyPair to "PDF", attachmentFileNamePair to newFileName),
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
        AllItemsDbRowCreator.createOrUpdate(item, database)
        AllItemsDbRowCreator.createOrUpdate(parentItem, database)
    }

    private fun getValidFileName(fileName: String): String {
        var fileName = fileName.replace(Regex("[\\/\\\\\\?\\*:|\"<>]"), "")
        // Replace newlines and tabs (which shouldn't be in the string in the first place) with spaces
        fileName = fileName.replace(Regex("[\\r\\n\\t]+"), " ")
        // Replace various thin spaces
        fileName = fileName.replace(Regex("[\\u2000-\\u200A]"), " ")
        // Replace zero-width spaces
        fileName = fileName.replace(Regex("[\\u200B-\\u200E]"), "")
        // Replace line and paragraph separators
        fileName = fileName.replace(Regex("[\\u2028-\\u2029]"), " ")

        // Strip characters not valid in XML, since they won't sync and they're probably unwanted
        // eslint-disable-next-line no-control-regex
        fileName = fileName.replace(
            Regex("[\\u0000-\\u0008\\u000b\\u000c\\u000e-\\u001f\\ud800-\\udfff\\ufffe\\uffff]"),
            ""
        )

        // Replace bidi isolation control characters
        fileName = fileName.replace(Regex("[\\u2068\\u2069]"), "")
        // Don't allow hidden files
        fileName = fileName.replace(Regex("^\\."), "")
        // Don't allow blank or illegal filenames
        if (fileName == null || fileName == "." || fileName == "..") {
            fileName = "_"
        }
        return fileName
    }

    private fun getFileBaseNameFromItem(item: RItem): String {
        val title =
            item.fields.where().key(FieldKeys.Item.title).findFirst()!!.value
        val year = if (item.parsedDate == null) "" else item.parsedYear.toString()
        val creator = item.creatorSummary ?: ""

        return "${creator}${if (creator.isNotEmpty()) " - " else ""}${year}${if (year.isNotEmpty()) " - " else ""}${
            title.take(100)
        }"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("itemKey") itemKey: String,
            @Assisted("parentItemKey") parentItemKey: String
        ): LinkAttachmentToParentItemDbRequest
    }
}