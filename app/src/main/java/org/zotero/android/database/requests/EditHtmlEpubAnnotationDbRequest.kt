package org.zotero.android.database.requests

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.sync.LibraryIdentifier

class EditHtmlEpubAnnotationDbRequest(
    private val libraryId: LibraryIdentifier,
    private val annotation: NewReaderAnnotation,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(annotation.key, libraryId).findFirst() ?: return
        if (item.deleted) {
            return
        }

        val fieldValues = mapOf(
            FieldKeys.Item.Annotation.color to annotation.color,
            FieldKeys.Item.Annotation.comment to annotation.comment,
            FieldKeys.Item.Annotation.text to (annotation.text ?: ""),
            FieldKeys.Item.Annotation.pageLabel to annotation.pageLabel,
            FieldKeys.Item.Annotation.sortIndex to annotation.sortIndex,
        )
        var didChange = false
        for ((key, value) in fieldValues) {
            if (syncField(key = key, value = value, item = item, database = database)) {
                didChange = true
            }
        }
        if (syncPosition(item = item, database = database)) {
            didChange = true
        }

        if (!didChange) {
            return
        }

        item.annotationSortIndex = annotation.sortIndex
        item.htmlFreeContent =
            if (annotation.comment.isEmpty()) null else annotation.comment.strippedRichTextTags
        item.dateModified = annotation.dateModified
        item.changeType = UpdatableChangeType.user.name
        item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.fields)))
    }

    private fun syncField(key: String, value: String, item: RItem, database: Realm): Boolean {
        val field = item.fields.where().key(key).isNull("baseKey").findFirst()
        if (field == null) {
            if (value.isEmpty()) {
                return false
            }
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = key
            rField.value = value
            rField.changed = true
            return true
        }
        if (field.value == value) {
            return false
        }
        field.value = value
        field.changed = true
        return true
    }

    private fun syncPosition(item: RItem, database: Realm): Boolean {
        val existingFields =
            item.fields.where().baseKey(FieldKeys.Item.Annotation.position).findAll()
        if (positionOf(existingFields) == annotation.position) {
            return false
        }
        existingFields.deleteAllFromRealm()
        for ((key, value) in annotation.position.entrySet()) {
            if (value is JsonNull) {
                continue
            }
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = key
            rField.value = positionValueToString(value)
            rField.baseKey = FieldKeys.Item.Annotation.position
            rField.changed = true
        }
        return true
    }

    private fun positionOf(fields: List<RItemField>): JsonObject {
        val position = JsonObject()
        for (field in fields) {
            if (field.value.firstOrNull() == '{') {
                val parsed = try {
                    JsonParser.parseString(field.value)
                } catch (e: Exception) {
                    JsonPrimitive(field.value)
                }
                position.add(field.key, parsed)
            } else {
                position.addProperty(field.key, field.value)
            }
        }
        return position
    }

    private fun positionValueToString(value: JsonElement): String {
        return if (value.isJsonPrimitive) value.asString else value.toString()
    }
}