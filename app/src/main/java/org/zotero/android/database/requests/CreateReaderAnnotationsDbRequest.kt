package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RItemField
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RUser
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.screens.htmlepub.reader.data.ReaderAnnotation
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber

open class CreateReaderAnnotationsDbRequest<Annotation: ReaderAnnotation>(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
    private val annotations: List<Annotation>,
    private val userId: Long,
    private val schemaController: SchemaController,
): DbRequest {
    override val needsWrite: Boolean
        get() {
            return true
        }

    override fun process(database: Realm) {
        val parent = database.where<RItem>().key(key = attachmentKey, libraryId = libraryId).findFirst() ?: return

        for (annotation in annotations) {
            create(annotation = annotation, parent = parent, database = database)
        }
    }

    fun create(annotation: Annotation, parent: RItem, database: Realm) {
        val fromRestore: Boolean
        val item: RItem
        val _item = database.where<RItem>().key(key = annotation.key, libraryId = libraryId).findFirst()
        if (_item != null) {
            if (!_item.deleted) {
                return
            }

            // If item exists and was already deleted locally and not yet synced, we re-add the item
            item = _item
            item.deleted = false
            fromRestore = true
        } else {
            item = database.createObject<RItem>()
            item.key = annotation.key
            item.rawType = ItemTypes.annotation
            item.localizedType = schemaController.localizedItemType(ItemTypes.annotation) ?: ""
            item.libraryId = libraryId
            item.dateAdded = annotation.dateAdded
            fromRestore = false
        }

        item.annotationType = annotation.type.name
        item.syncState = ObjectSyncState.synced.name
        item.changeType = UpdatableChangeType.user.name
        item.htmlFreeContent =
            if (annotation.comment.isEmpty()) {
                null
            } else {
                annotation.comment.strippedRichTextTags
            }
        item.dateModified = annotation.dateModified
        item.parent = parent

        if (annotation.isAuthor(currentUserId = userId)) {
            val user = database.where<RUser>().equalTo("identifier", userId).findFirst()
            item.createdBy = user
            if (user == null) {
                Timber.w("CreateReaderAnnotationsDbRequest: user not found for userId $userId when creating annotation ${annotation.key} in library $libraryId)")
            }
        }

        addFields(annotation = annotation, item = item, database = database)
        addTags(annotation = annotation, item = item, database = database)
        // We need to submit tags on creation even if they are empty, so we need to mark them as changed
        val changes = mutableListOf(RItemChanges.parent, RItemChanges.fields, RItemChanges.type, RItemChanges.tags)
        addAdditionalProperties(annotation, fromRestore = fromRestore, item, changes, database)
        item.changes.add(RObjectChange.create(changes = changes))
    }


    open fun addFields(annotation: Annotation, item: RItem, database: Realm) {
        for (field in FieldKeys.Item.Annotation.mandatoryApiFields(annotation.type)) {
            val rField = database.createEmbeddedObject(RItemField::class.java, item, "fields")
            rField.key = field.key
            rField.baseKey = field.baseKey
            rField.changed = true

            when(field.key) {
                FieldKeys.Item.Annotation.type -> {
                    rField.value = annotation.type.name
                }

                FieldKeys.Item.Annotation.color -> {
                    rField.value = annotation.color
                }

                FieldKeys.Item.Annotation.comment -> {
                    rField.value = annotation.comment
                }

                FieldKeys.Item.Annotation.sortIndex -> {
                    rField.value = annotation.sortIndex
                    item.annotationSortIndex = annotation.sortIndex
                }

                FieldKeys.Item.Annotation.text -> {
                rField.value = annotation.text ?: ""
                }
            }
        }
    }


    open fun addTags(annotation: Annotation, item: RItem, database: Realm) { }

    open fun addAdditionalProperties(annotation: Annotation, fromRestore: Boolean, item: RItem, changes: MutableList<RItemChanges>, database: Realm) { }

}