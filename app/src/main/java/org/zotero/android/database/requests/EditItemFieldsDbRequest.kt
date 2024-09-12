package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.androidx.text.strippedHtmlTags
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class EditItemFieldsDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val fieldValues: Map<KeyBaseKeyPair, String>,
    private val dateParser: DateParser,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        if (this.fieldValues.isEmpty()) {
            return
        }
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst() ?: return
        var didChange = false
        for (data in this.fieldValues) {
            val field = if (data.key.baseKey != null) {
                item.fields.where().key(data.key.key, andBaseKey = data.key.baseKey!!)
            } else {
                item.fields.where().key(data.key.key)
            }.findFirst()

            if (field == null || data.value == field.value) {
                continue
            }

            field.value = data.value
            field.changed = true
            didChange = true

            when {
                field.key == FieldKeys.Item.note -> {
                    item.htmlFreeContent = if (data.value.isEmpty()) {
                        null
                    } else {
                        data.value.strippedHtmlTags
                    }
                }
                field.key == FieldKeys.Item.Annotation.comment -> {
                    item.htmlFreeContent =
                        if (data.value.isEmpty()) null else data.value.strippedRichTextTags
                }
                field.key == FieldKeys.Item.title || field.baseKey == FieldKeys.Item.title  -> {
                    item.set(title = field.value)
                }
                field.key == FieldKeys.Item.date || field.baseKey == FieldKeys.Item.date  -> {
                    item.setDateFieldMetadata(field.value, parser = dateParser)
                }
                field.key == FieldKeys.Item.publisher || field.baseKey == FieldKeys.Item.publisher  -> {
                    item.setP(publisher = field.value)
                }
                field.key == FieldKeys.Item.publicationTitle || field.baseKey == FieldKeys.Item.publicationTitle  -> {
                    item.setPT(publicationTitle = field.value)
                }

            }
        }

        if (didChange) {
            item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.fields)))
            item.changeType = UpdatableChangeType.user.name
            item.dateModified = Date()
        }
    }
}