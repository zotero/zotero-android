package org.zotero.android.architecture.database.objects

import com.google.gson.Gson
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.RealmClass
import io.realm.kotlin.where
import org.zotero.android.architecture.database.requests.ReadBaseTagsToDeleteDbRequest
import org.zotero.android.architecture.database.requests.baseKey
import org.zotero.android.architecture.database.requests.key
import org.zotero.android.architecture.database.requests.nameIn
import org.zotero.android.formatter.ItemTitleFormatter
import org.zotero.android.formatter.sqlFormat
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.CreatorSummaryFormatter
import org.zotero.android.sync.LinkMode
import java.util.Date

enum class RItemChanges {
    type,
    trash,
    parent,
    collections,
    fields,
    tags,
    creators,
    relations,
    rects,
    paths,
}

open class RItem : Updatable, Deletable, Syncable, RealmObject() {

    companion object {
        val observableKeypathsForItemList = listOf(
            "rawType",
            "baseTitle",
            "displayTitle",
            "sortTitle",
            "creatorSummary",
            "sortCreatorSummary",
            "hasCreatorSummary",
            "parsedDate",
            "hasParsedDate",
            "parsedYear",
            "hasParsedYear",
            "publisher",
            "hasPublisher",
            "publicationTitle",
            "hasPublicationTitle",
            "children.backendMd5",
            "tags"
        )
        val observableKeypathsForItemDetail = listOf("version", "changeType", "children.version")
    }

    @Index
    override var key: String = ""
    var rawType: String = ""
    var baseTitle: String = ""
    var inPublications: Boolean = false
    lateinit var dateAdded: Date
    lateinit var dateModified: Date
    var parent: RItem? = null
    var createdBy: RUser? = null
    var lastModifiedBy: RUser? = null
    override var customLibraryKey: String? = null//RCustomLibraryType
    override var groupKey: Int? = null

    @LinkingObjects("items")
    val collections: RealmResults<RCollection>? = null

    var fields: RealmList<RItemField> = RealmList()

    @LinkingObjects("parent")
    val children: RealmResults<RItem>? = null

    @LinkingObjects("item")
    val tags: RealmResults<RTypedTag>? = null

    var creators:RealmList<RCreator> = RealmList()
    var links:RealmList<RLink> = RealmList()
    var relations:RealmList<RRelation> = RealmList()

    var backendMd5: String = ""
    var fileDownloaded: Boolean = false
    var rects: RealmList<RRect> = RealmList()
    var paths: RealmList<RPath> = RealmList()
    var localizedType: String = ""
    var displayTitle: String = ""
    var sortTitle: String = ""
    var creatorSummary: String? = null
    var sortCreatorSummary: String? = null
    var hasCreatorSummary: Boolean = false
    var parsedDate: Date? = null
    var hasParsedDate: Boolean = false
    var parsedYear: Int = 0
    var hasParsedYear: Boolean = false
    var publisher: String? = null
    var hasPublisher: Boolean = false
    var publicationTitle: String? = ""
    var hasPublicationTitle: Boolean = false

    @Index
    var annotationSortIndex: String = ""
    var trash: Boolean = false

    @Index
    override var version: Int = 0
    var attachmentNeedsSync: Boolean = false
    override var syncState: String = "" //ObjectSyncState
    override lateinit var lastSyncDate: Date
    override var syncRetries: Int = 0
    override var changes: RealmList<RObjectChange> = RealmList()
    override lateinit var changeType: String //UpdatableChangeType
    override var deleted: Boolean = false

    val doi: String?
        get() {
            val fieldS = fields.filter { it.key == FieldKeys.Item.doi }.firstOrNull()
            if (fieldS == null) {
                return null
            }
            val doi = FieldKeys.Item.clean(doi = fieldS.value)
            return if (!doi.isEmpty()) doi else null
        }

    val urlString: String?
        get() {
            return fields.filter { it.key == FieldKeys.Item.url }.firstOrNull()?.value
        }

//    @Ignore
    val changedFields: List<RItemChanges>
        get() {
            return changes.flatMap { it.rawChanges.map { RItemChanges.valueOf(it) } }
        }


    fun set(title: String) {
        baseTitle = title
        updateDerivedTitles()
    }

    fun setPT(publicationTitle: String?) {
        this.publicationTitle = publicationTitle?.lowercase()
        this.hasPublicationTitle = publicationTitle?.isEmpty() == false
    }

    fun setP(publisher: String?) {
        this.publisher = publisher?.lowercase()
        this.hasPublisher = publisher?.isEmpty() == false
    }

    fun updateDerivedTitles() {
        val displayTitle = ItemTitleFormatter.displayTitle(this)
        if (this.displayTitle != displayTitle) {
            this.displayTitle = displayTitle
        }
        updateSortTitle()
    }

    private fun updateSortTitle() {
        val newTitle = displayTitle.trim('[', ']', '\'', '"').lowercase()
        if (newTitle != this.sortTitle) {
            sortTitle = newTitle
        }
    }

    override val updateParameters: Map<String, Any>?
        get() {
            if (!isChanged) {
                return null
            }
            var positionFieldChanged = false
            val parameters: MutableMap<String, Any> = mutableMapOf(
                "key" to this.key,
                "version" to this.version,
                "dateModified" to sqlFormat.format(dateModified),
                "dateAdded" to sqlFormat.format(dateAdded)
            )

            val changes = this.changedFields
            if (changes.contains(RItemChanges.type)) {
                parameters["itemType"] = this.rawType
            }
            if (changes.contains(RItemChanges.trash)) {
                parameters["deleted"] = this.trash
            }
            if (changes.contains(RItemChanges.tags)) {
                parameters["tags"] =
                    this.tags!!.map { listOf("tag" to it.tag?.name, "type" to it.type) }
                        .toTypedArray()
            }
            if (changes.contains(RItemChanges.collections)) {
                parameters["collections"] =
                    collections?.map { it.key }?.toTypedArray() ?: emptyArray<String>()
            }
            if (changes.contains(RItemChanges.relations)) {
                val relations = mutableMapOf<String, String>()
                this.relations.forEach { relation ->
                    relations[relation.type] = relation.urlString
                }
                parameters["relations"] = relations
            }
            if (changes.contains(RItemChanges.parent)) {
                parameters["parentItem"] = this.parent?.key ?: false
            }
            if (changes.contains(RItemChanges.creators)) {
                parameters["creators"] = this.creators.map { it.updateParameters }.toTypedArray()
            }
            if (changes.contains(RItemChanges.fields)) {
                for (field in this.fields.filter { it.changed == true }) {
                    if (field.baseKey == FieldKeys.Item.Annotation.position) {
                        positionFieldChanged = true
                        continue
                    }

                    when (field.key) {
                        FieldKeys.Item.Attachment.md5, FieldKeys.Item.Attachment.mtime ->
                            parameters[field.key] = ""
                        else ->
                            parameters[field.key] = field.value
                    }
                }
            }


            if (this.rawType == ItemTypes.annotation && (changes.contains(RItemChanges.rects)
                        || changes.contains(RItemChanges.paths) || positionFieldChanged)
            ) {
                val annotationType =
                    this.fields.where().key(FieldKeys.Item.Annotation.type).findFirst()
                        ?.let { AnnotationType.valueOf(it.value) }
                if (annotationType != null) {
                    parameters[FieldKeys.Item.Annotation.position] = createAnnotationPosition(
                        annotationType,
                        this.fields.where().baseKey(FieldKeys.Item.Annotation.position).findAll()
                    )

                }
            }
            return parameters
        }

    private fun createAnnotationPosition(
        type: AnnotationType,
        positionFields: RealmResults<RItemField>
    ): String {
        val jsonData = mutableMapOf<String, Any>()

        for (field in positionFields) {
            val value = field.value.toIntOrNull()
            if (value != null) {
                jsonData[field.key] = value
            } else {
                val doubleVal = field.value.toDoubleOrNull()
                if (doubleVal != null) {
                    jsonData[field.key] = doubleVal
                } else {
                    jsonData[field.key] = field.value
                }
            }
        }

        when (type) {
            AnnotationType.ink -> {
                val apiPaths: MutableList<List<Double>> = mutableListOf()
                for (path in this.paths.sortedBy { it.sortIndex }) {
                    apiPaths.add(path.coordinates!!
                        .sortedBy { it.sortIndex }
                        .map { it.value.rounded(3) })
                }

                jsonData[FieldKeys.Item.Annotation.Position.paths] = apiPaths
            }
            AnnotationType.highlight, AnnotationType.image, AnnotationType.note -> {
                val rectArray = mutableListOf<List<Double>>()
                this.rects.forEach { rRect ->
                    rectArray.add(
                        listOf(
                            rRect.minX.rounded(3),
                            rRect.minY.rounded(3),
                            rRect.maxX.rounded(3),
                            rRect.maxY.rounded(3)
                        )
                    )
                }
                jsonData[FieldKeys.Item.Annotation.Position.rects] = rectArray
            }
        }
        return Gson().toJson(jsonData)
    }

    override val selfOrChildChanged: Boolean
        get() {
            if (this.isChanged) {
                return true
            }

            for (child in this.children!!) {
                if (child.selfOrChildChanged) {
                    return true
                }
            }

            return false
        }

    override fun markAsChanged(database: Realm) {
        this.changes.add(RObjectChange.create(changes = this.currentChanges))
        this.changeType = UpdatableChangeType.user.name
        this.deleted = false
        this.version = 0

        for (field in this.fields) {
            if (field.value.isEmpty()) {
                continue
            }
            field.changed = true
        }

        val hasLinkModeField = this.fields.filter { it.key == FieldKeys.Item.Attachment.linkMode }
            .firstOrNull()?.value == LinkMode.importedFile.name

        if (this.rawType == ItemTypes.attachment && hasLinkModeField) {
            this.attachmentNeedsSync = true
        }

        this.children!!.forEach { child ->
            child.markAsChanged(database)
        }
    }

    private val currentChanges: List<RItemChanges>
        get() {
            var changes = mutableListOf(RItemChanges.type, RItemChanges.fields)
            if (!this.creators.isEmpty()) {
                changes.add(RItemChanges.creators)
            }
            if (this.collections.isNullOrEmpty()) {
                changes.add(RItemChanges.collections)
            }
            if (this.parent != null) {
                changes.add(RItemChanges.parent)
            }
            if (!this.tags!!.isEmpty()) {
                changes.add(RItemChanges.tags)
            }
            if (this.trash) {
                changes.add(RItemChanges.trash)
            }
            if (!this.relations.isEmpty()) {
                changes.add(RItemChanges.relations)
            }
            if (!this.rects.isEmpty()) {
                changes.add(RItemChanges.rects)
            }
            if (!this.paths.isEmpty()) {
                changes.add(RItemChanges.paths)
            }
            return changes
        }


    override fun willRemove(database: Realm) {
        if (this.children!!.isValid) {
            for (child in this.children) {
                if (!child.isValid) {
                    continue
                }
                child.willRemove(database)
            }
            children.deleteAllFromRealm()
        }
        if (this.tags!!.isValid) {
            val baseTagsToRemove = ReadBaseTagsToDeleteDbRequest(this.tags).process(
                database,
            ) ?: emptyList()
            this.tags.deleteAllFromRealm()
            if (!baseTagsToRemove.isEmpty()) {
                database.where<RTag>().nameIn(baseTagsToRemove).findAll().deleteAllFromRealm()
            }
        }

        val createdByUser = this.createdBy
        val lastModifiedByUser = this.lastModifiedBy

        if (createdByUser != null &&
            createdByUser.isValid &&
            lastModifiedByUser != null &&
            lastModifiedByUser.isValid &&
            createdByUser.identifier == lastModifiedByUser.identifier &&
            createdByUser.createdBy.count() == 1 &&
            createdByUser.modifiedBy.count() == 1
        ) {
            createdByUser.deleteFromRealm()
        } else {
            val userCreatedBy = this.createdBy
            if (userCreatedBy != null &&
                userCreatedBy.isValid &&
                userCreatedBy.createdBy.count() == 1 &&
                (!userCreatedBy.modifiedBy.isValid || userCreatedBy.modifiedBy.isEmpty())
            ) {
                userCreatedBy.deleteFromRealm()
            }
            val user = this.lastModifiedBy
            if (user != null && user.isValid &&
                (!user.createdBy.isValid || user.createdBy.isEmpty()) &&
                user.modifiedBy.count() == 1
            ) {
                user.deleteFromRealm()
            }
        }


        when (this.rawType) {
            ItemTypes.attachment -> {
                deletePageIndex(database)
                cleanupAttachmentFiles()
            }
            ItemTypes.annotation -> {
                cleanupAnnotationFiles()
            }
            else -> {}
        }
    }

    override val isInvalidated: Boolean
        get() = !isValid

    private fun deletePageIndex(database: Realm) {
        val libraryId = this.libraryId
        if (libraryId != null) {
            database.where<RPageIndex>().key(key, libraryId).findFirst()?.deleteFromRealm()
        }
    }

    private fun cleanupAnnotationFiles() {
        //TODO cleanup annotations & fire event bus events.
    }

    private fun cleanupAttachmentFiles() {
        //TODO cleanup attachments & fire event bus events.
    }

    override fun deleteChanges(uuids: List<String>, database: Realm) {
        this.changes.filter { uuids.contains(it.identifier) }.forEach {
            it.deleteFromRealm()
        }

        this.changeType = UpdatableChangeType.syncResponse.name
        this.fields.filter { it.changed }.forEach { field ->
            field.changed = false
        }
    }

    override fun deleteAllChanges(database: Realm) {
        if (!this.isChanged) {
            return
        }

        this.changes.deleteAllFromRealm()
        this.changeType = UpdatableChangeType.sync.name
        this.fields.filter { it.changed }.forEach { field ->
            field.changed = false
        }
    }

    fun updateCreatorSummary() {
        creatorSummary = CreatorSummaryFormatter.summary(this.creators)
        sortCreatorSummary = this.creatorSummary?.lowercase()
        hasCreatorSummary = this.creatorSummary != null
    }

}

@RealmClass(embedded = true)
open class RItemField : RealmObject() {
    var key: String = ""
    var baseKey: String? = null
    var value: String = ""
    var changed: Boolean = false
}
