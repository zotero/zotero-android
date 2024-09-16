package org.zotero.android.database.objects

import com.google.gson.JsonObject
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.RealmClass
import io.realm.kotlin.where
import org.greenrobot.eventbus.EventBus
import org.zotero.android.ZoteroApplication
import org.zotero.android.androidx.text.strippedRichTextTags
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.database.requests.ReadBaseTagsToDeleteDbRequest
import org.zotero.android.database.requests.baseKey
import org.zotero.android.database.requests.key
import org.zotero.android.database.requests.nameIn
import org.zotero.android.helpers.formatter.ItemTitleFormatter
import org.zotero.android.helpers.formatter.sqlFormat
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.CreatorSummaryFormatter
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.LinkMode
import timber.log.Timber
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
    var annotationType: String = ""

    @Index
    var annotationSortIndex: String = ""
    var trash: Boolean = false

    @Index
    override var version: Int = 0
    var attachmentNeedsSync: Boolean = false
    override var syncState: String = "" //ObjectSyncState
    override var lastSyncDate: Date? = null
    override var syncRetries: Int = 0
    override var changes: RealmList<RObjectChange> = RealmList()
    var changesSyncPaused: Boolean = false
    override lateinit var changeType: String //UpdatableChangeType
    override var deleted: Boolean = false
    var htmlFreeContent: String? = null
    var allItemsDbRow: AllItemsDbRow? = null

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
        val newTitle = displayTitle.strippedRichTextTags.trim('[', ']', '\'', '"').lowercase()
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
                parameters["tags"] = this.tags!!.map {
                    mapOf(
                        "tag" to it.tag?.name,
                        "type" to RTypedTag.Kind.valueOf(it.type).int
                    )
                }
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
                parameters["creators"] = this.creators.sort("orderId").map { it.updateParameters }.toTypedArray()
            }
            if (changes.contains(RItemChanges.fields)) {
                for (field in this.fields.filter { it.changed }) {
                    if (field.baseKey == FieldKeys.Item.Annotation.position) {
                        positionFieldChanged = true
                        continue
                    }

                    when (field.key) {
                        FieldKeys.Item.Attachment.mtime,
                        FieldKeys.Item.Attachment.md5 ->
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
                        type = annotationType,
                        positionFields = this.fields.where().baseKey(FieldKeys.Item.Annotation.position).findAll(),
                    )

                }
            }
            return parameters
        }

    private fun createAnnotationPosition(
        type: AnnotationType,
        positionFields: RealmResults<RItemField>,
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
                    try {
                        val json = ZoteroApplication.instance.gson.fromJson(field.value, JsonObject::class.java)
                        jsonData[field.key] = json
                    } catch (e: Exception) {
                        Timber.w(e)//This is not a bug, but just for debug purposes
                        jsonData[field.key] = field.value
                    }
                }
            }
        }

        when (type) {
            AnnotationType.ink -> {
                val apiPaths: MutableList<List<Double>> = mutableListOf()
                for (path in this.paths.sortedBy { it.sortIndex }) {
                    apiPaths.add(path.coordinates
                        .sortedBy { it.sortIndex }
                        .map { it.value })
                }

                jsonData[FieldKeys.Item.Annotation.Position.paths] = apiPaths
            }

            AnnotationType.highlight, AnnotationType.image, AnnotationType.note, AnnotationType.underline, AnnotationType.text -> {
                val rectArray = mutableListOf<List<Double>>()
                this.rects.forEach { rRect ->
                    rectArray.add(
                        listOf(
                            rRect.minX,
                            rRect.minY,
                            rRect.maxX,
                            rRect.maxY
                        )
                    )
                }
                jsonData[FieldKeys.Item.Annotation.Position.rects] = rectArray
            }
        }
        return ZoteroApplication.instance.gsonWithRoundedDecimals.toJson(jsonData)
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
        this.changes.add(RObjectChange.create(changes = this.allChanges))
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

    private val allChanges: List<RItemChanges>
        get() {
            if (this.rawType == ItemTypes.annotation) {
                val changes = mutableListOf(
                    RItemChanges.parent,
                    RItemChanges.fields,
                    RItemChanges.type,
                    RItemChanges.tags
                )
                if (!this.rects.isEmpty()) {
                    changes.add(RItemChanges.rects)
                }
                if (!this.paths.isEmpty()) {
                    changes.add(RItemChanges.paths)
                }
                return changes
            }


            val changes = mutableListOf(RItemChanges.type, RItemChanges.fields, RItemChanges.tags)
            if (!this.creators.isEmpty()) {
                changes.add(RItemChanges.creators)
            }
            if (this.collections.isNullOrEmpty()) {
                changes.add(RItemChanges.collections)
            }
            if (this.parent != null) {
                changes.add(RItemChanges.parent)
            }
            if (this.trash) {
                changes.add(RItemChanges.trash)
            }
            if (!this.relations.isEmpty()) {
                changes.add(RItemChanges.relations)
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
            )
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
            createdByUser.createdBy!!.count() == 1 &&
            createdByUser.modifiedBy!!.count() == 1
        ) {
            createdByUser.deleteFromRealm()
        } else {
            val userCreatedBy = this.createdBy
            if (userCreatedBy != null &&
                userCreatedBy.isValid &&
                userCreatedBy.createdBy!!.count() == 1 &&
                (!userCreatedBy.modifiedBy!!.isValid || userCreatedBy.modifiedBy.isEmpty())
            ) {
                userCreatedBy.deleteFromRealm()
            }
            val user = this.lastModifiedBy
            if (user != null && user.isValid &&
                (!user.createdBy!!.isValid || user.createdBy.isEmpty()) &&
                user.modifiedBy!!.count() == 1
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
        val parentKey = this.parent?.key
        val libraryId = this.libraryId
        if (parentKey == null || libraryId == null) {
            return
        }

        val fileStorage = ZoteroApplication.instance.fileStore
        val light = fileStorage.annotationPreview(annotationKey = this.key, pdfKey = parentKey, libraryId = libraryId, isDark =false)
        val dark = fileStorage.annotationPreview(annotationKey = this.key, pdfKey = parentKey, libraryId = libraryId, isDark = true)

        EventBus.getDefault().post(EventBusConstants.AttachmentDeleted(light))
        EventBus.getDefault().post(EventBusConstants.AttachmentDeleted(dark))
    }

    private fun cleanupAttachmentFiles() {
        val fileStorage = ZoteroApplication.instance.fileStore
        val type = AttachmentCreator.attachmentType(
            item = this,
            options = AttachmentCreator.Options.light,
            fileStorage = fileStorage,
            urlDetector = null,
            isForceRemote = true,
            defaults = ZoteroApplication.instance.defaults,
        )
        if (type == null) {
            return
        }
        when (type) {
            is Attachment.Kind.url -> {
                //no-op
            }
            is Attachment.Kind.file -> {
                val contentType = type.contentType
                val linkType = type.linkType
                val libraryId = this.libraryId
                if (linkType == Attachment.FileLinkType.linkedFile || libraryId == null) {
                    return
                }

                EventBus.getDefault().post(
                    EventBusConstants.AttachmentDeleted(
                        fileStorage.attachmentDirectory(
                            libraryId,
                            this.key
                        )
                    )
                )

                if (contentType == "application/pdf") {
                    EventBus.getDefault().post(
                        EventBusConstants.AttachmentDeleted(
                            fileStorage.annotationPreviews(
                                this.key,
                                libraryId = libraryId
                            )
                        )
                    )
                }
            }
        }
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

    fun setDateFieldMetadata(date: String, parser: DateParser) {
        val components = parser.parse(date)
        this.parsedYear = components?.year ?: 0
        this.hasParsedYear = this.parsedYear != 0
        this.parsedDate = components?.date
        this.hasParsedDate = this.parsedDate != null
    }

    fun clearDateFieldMedatada() {
        this.parsedYear = 0
        this.hasParsedYear = false
        this.parsedDate = null
        this.hasParsedDate = false
    }

    fun fieldValue(key: String): String? {
        val value = this.fields.where().key(key).findFirst()?.value
        if (value == null) {
            Timber.e("DatabaseAnnotation: missing value for `$key`")
        }
        return value
    }

    val mtimeAndHashParameters: Map<String, Any>
        get() {
            val parameters: MutableMap<String, Any> = mutableMapOf(
                "key" to this.key,
                "version" to this.version,
                "dateModified" to sqlFormat.format(this.dateModified),
                "dateAdded" to sqlFormat.format(this.dateAdded)
            )
            val md5 = this.fields.where().key(FieldKeys.Item.Attachment.md5).findFirst()?.value
            if (md5 != null) {
                parameters[FieldKeys.Item.Attachment.md5] = md5
            }
            val mtime = this.fields.where().key(FieldKeys.Item.Attachment.mtime)
                .findFirst()?.value?.toLongOrNull()
            if (mtime != null) {
                parameters[FieldKeys.Item.Attachment.mtime] = mtime
            }
            return parameters
        }

}

@RealmClass(embedded = true)
open class RItemField : RealmObject() {
    var key: String = ""
    var baseKey: String? = null
    var value: String = ""
    var changed: Boolean = false
}
