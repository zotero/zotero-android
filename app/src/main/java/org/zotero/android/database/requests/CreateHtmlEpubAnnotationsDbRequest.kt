package org.zotero.android.database.requests

import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import com.google.gson.Gson
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.database.objects.RTypedTag.Kind
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController

class CreateHtmlEpubAnnotationsDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
    private val annotations: List<HtmlEpubAnnotation>,
    private val userId: Long,
    private val schemaController: SchemaController,
    private val gson: Gson,
) : CreateReaderAnnotationsDbRequest<HtmlEpubAnnotation>(
    attachmentKey = attachmentKey,
    libraryId = libraryId,
    annotations = annotations,
    userId = userId,
    schemaController = schemaController,
    ) {
    override fun addFields(annotation: HtmlEpubAnnotation, item: RItem, database: Realm) {
        super.addFields(annotation, item, database)

        for (field in FieldKeys.Item.Annotation.extraHtmlEpubFields(annotation.type)) {
            val value: String

            when (field.key) {
                FieldKeys.Item.Annotation.pageLabel -> {
                    value = annotation.pageLabel
                }

                else -> {
                    continue
                }
            }

            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseKey
            rField.changed = true
            rField.value = value
        }

        for ((key, value) in annotation.position) {
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = key
            rField.value = positionValueToString(value)
            rField.baseKey = FieldKeys.Item.Annotation.position
            rField.changed = true
        }
    }


    fun positionValueToString(value: Any): String {
        if (value is String) {
            return value
        }
        if (value is Map<*, *>) {
            return gson.toJson(value)
        }
        return "$value"
    }

    override fun addTags(annotation: HtmlEpubAnnotation, item: RItem, database: Realm) {
        val allTags = database.where<RTag>()

        for (tag in annotation.tags) {
            val rTag = allTags.name(tag.name).findFirst() ?:  continue

            val rTypedTag = database.createObject<RTypedTag>()
            rTypedTag.type = Kind.manual.name
            rTypedTag.item = item
            rTypedTag.tag = rTag
        }
    }
}