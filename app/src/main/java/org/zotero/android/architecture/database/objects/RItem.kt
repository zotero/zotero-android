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
import org.zotero.android.architecture.database.requests.key
import org.zotero.android.architecture.database.requests.name
import org.zotero.android.formatter.ItemTitleFormatter
import org.zotero.android.formatter.iso8601DateFormat
import org.zotero.android.ktx.rounded
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


open class RItem: Updatable, Deletable, Syncable, RealmObject() {

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
    val collections: RealmResults<RCollection> = TODO()

    lateinit var fields: RealmList<RItemField>

    @LinkingObjects("parent")
    val children: RealmResults<RItem> = TODO()

    @LinkingObjects("item")
    val tags: RealmResults<RTypedTag> = TODO()

    lateinit var creators: RealmList<RCreator>
    lateinit var links: RealmList<RLink>
    lateinit var relations: RealmList<RRelation>

    var backendMd5: String = ""
    var fileDownloaded: Boolean = false
    lateinit var rects: RealmList<RRect>
    lateinit var paths: RealmList<RPath>
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
    var annotationSortIndex: String = ""
    var trash: Boolean = false

    @Index
    override var version: Int = 0
    var attachmentNeedsSync: Boolean = false
    override lateinit var syncState: String //ObjectSyncState
    override lateinit var lastSyncDate: Date
    override var syncRetries: Int = 0
    override lateinit var rawChangedFields: RealmList<String>
    override lateinit var changeType: String //UpdatableChangeType
    override var deleted: Boolean = false

    val doi: String?
        get() {
            val fieldS = fields.filter { it.key == FieldKeys.Item.doi }.first()
            val doi = FieldKeys.Item.clean(doi = fieldS.value)
            return if (!doi.isEmpty()) doi else null
        }

    val urlString: String?
        get() {
            return fields.filter { it.key == FieldKeys.Item.url }.firstOrNull()?.value
        }

    var changedFields: List<RItemChanges>
        get() {
            return rawChangedFields.map { RItemChanges.valueOf(it) }
        }
        set(newValue) {
            val z = RealmList<String>()
            z.addAll(newValue.map { it.name })
            rawChangedFields = z
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
            var changedPageIndex: Int? = null
            var changedLineWidth: Double? = null
            var parameters: MutableMap<String, Any> = mutableMapOf(
                "key" to this.key,
                "version" to this.version,
                "dateModified" to iso8601DateFormat.format(dateModified),
                "dateAdded" to iso8601DateFormat.format(dateAdded)
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
                    this.tags.map { listOf("tag" to it.tag?.name, "type" to it.type) }
                        .toTypedArray()
            }
            if (changes.contains(RItemChanges.collections)) {
                parameters["collections"] = collections.map { it.key }.toTypedArray()
            }
            if (changes.contains(RItemChanges.relations)) {
                var relations = mutableMapOf<String, String>()
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
                this.fields.filter { it.changed == true }.forEach { field ->
                    when (field.key) {
                        FieldKeys.Item.Attachment.md5, FieldKeys.Item.Attachment.mtime ->
                            parameters[field.key] = ""
                        FieldKeys.Item.Annotation.pageIndex ->
                            changedPageIndex = field.value.toIntOrNull() ?: 0
                        FieldKeys.Item.Annotation.lineWidth ->
                            changedLineWidth = field.value.toDoubleOrNull() ?: 0.0
                        else ->
                            parameters[field.key] = field.value
                    }
                }
            }

            val annotationTypeS =
                this.fields.firstOrNull { it.key == FieldKeys.Item.Annotation.type }
            val annotationType =
                annotationTypeS?.let { AnnotationType.valueOf(annotationTypeS.value) }

            if (this.rawType == ItemTypes.annotation && (changes.contains(RItemChanges.rects)
                        || changes.contains(RItemChanges.paths)
                        || changedPageIndex != null
                        || changedLineWidth != null)
                && annotationType != null
            ) {
                parameters[FieldKeys.Item.Annotation.position] = this.createAnnotationPosition(
                    annotationType,
                    changedPageIndex = changedPageIndex,
                    changedLineWidth  = changedLineWidth
                )
            }
            val annotationType2 = this.fields.filter {it.key == FieldKeys.Item.Annotation.type}.firstOrNull()?.let{
                AnnotationType.valueOf(it.value)
            }


            if (this.rawType == ItemTypes.annotation && (changes.contains(RItemChanges.rects) || changes.contains(RItemChanges. paths) || changedPageIndex != null || changedLineWidth != null) &&
                annotationType2 != null) {
                parameters[FieldKeys.Item.Annotation.position] = this.createAnnotationPosition(
                    annotationType2,
                    changedPageIndex =  changedPageIndex,
                    changedLineWidth =  changedLineWidth
                )
            }

            return parameters
        }

    private fun createAnnotationPosition(
        type: AnnotationType,
        changedPageIndex: Int?,
        changedLineWidth: Double?
    ): String {
        val pageIndex = changedPageIndex
            ?: (this.fields.filter { it.key == FieldKeys.Item.Annotation.pageIndex }.firstOrNull()
                ?.let { it.value.toInt() } ?: 0)
        var jsonData: MutableMap<String, Any> =
            mutableMapOf(FieldKeys.Item.Annotation.pageIndex to pageIndex)

        when (type) {
            AnnotationType.ink -> {
                val lineWidth = changedLineWidth
                    ?: (this.fields.filter { it.key == FieldKeys.Item.Annotation.lineWidth }
                        .firstOrNull()?.let { it.value.toDouble() } ?: 0.0)
                var apiPaths: MutableList<List<Double>> = mutableListOf()
                for (path in this.paths.sortedBy { it.sortIndex }) {
                    apiPaths.add(path.coordinates.sortedBy { it.sortIndex }
                        .map { it.value.rounded(3) })
                }

                jsonData[FieldKeys.Item.Annotation.paths] = apiPaths
                jsonData[FieldKeys.Item.Annotation.lineWidth] = lineWidth.rounded(3)
            }
            AnnotationType.highlight, AnnotationType.image, AnnotationType.note -> {
                var rectArray = mutableListOf<List<Double>>()
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
                jsonData[FieldKeys.Item.Annotation.rects] = rectArray
            }
        }
        return Gson().toJson(jsonData)
    }

    override val selfOrChildChanged: Boolean
        get() = TODO("Not yet implemented")

    override fun markAsChanged(database: Realm) {
        this.changedFields = this.currentChanges
        this.changeType = UpdatableChangeType.user.name
                this.deleted = false
        this.version = 0

        for (field in this.fields) {
            if (field.value.isEmpty()) {
                continue
            }
            field.changed = true
        }

        if (this.rawType == ItemTypes.attachment && this.fields.filter(.key(FieldKeys.Item.Attachment.linkMode)).first?.value == LinkMode.importedFile.rawValue {
            this.attachmentNeedsSync = true
        }

        this.children.forEach { child ->
                child.markAsChanged(database)
        }
    }

    private val currentChanges: List<RItemChanges> get(){
        var changes = mutableListOf(RItemChanges.type, RItemChanges.fields)
        if (!this.creators.isEmpty()) {
            changes.add(RItemChanges.creators)
        }
        if (this.collections.isEmpty()) {
            changes.add(RItemChanges.collections)
        }
        if (this.parent != null) {
            changes.add(RItemChanges.parent)
        }
        if (!this.tags.isEmpty()) {
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
        if (this.children.isValid) {
            for (child in this.children) {
                if (!child.isValid) {
                    continue
                }
                child.willRemove(database)
            }
            children.deleteAllFromRealm()
        }
        if (this.tags.isValid) {
            val baseTagsToRemove = ReadBaseTagsToDeleteDbRequest<RTypedTag>(this.tags).process(database,RTypedTag::class ) ?: emptyList()
                this.tags.deleteAllFromRealm()
                if (!baseTagsToRemove.isEmpty()) {
                    database.where<RTag>().name(baseTagsToRemove).findAll().deleteAllFromRealm()
                }
            }

        val createdByUser = this.createdBy
        val lastModifiedByUser = this.lastModifiedBy

        if (createdByUser != null && createdByUser.isValid && lastModifiedByUser != null && lastModifiedByUser.isValid &&
            createdByUser.identifier == lastModifiedByUser.identifier &&
            createdByUser.createdBy.count() == 1 &&
            createdByUser.modifiedBy.count() == 1
        ) {
            createdByUser.deleteFromRealm()
        } else {
            val userCreatedBy = this.createdBy
            if (userCreatedBy != null && userCreatedBy.isValid && userCreatedBy.createdBy.count() == 1 && (!userCreatedBy.modifiedBy.isValid || userCreatedBy.modifiedBy.isEmpty())) {
                userCreatedBy.deleteFromRealm()
            }
            val user = this.lastModifiedBy
            if (user != null && user.isValid && (!user.createdBy.isValid || user.createdBy.isEmpty()) && user.modifiedBy.count() == 1) {
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

    private fun deletePageIndex(database: Realm) {
        val libraryId = this.libraryId
        if (libraryId != null) {
            val pageIndex = database.where<RPageIndex>().key(this.key, libraryId).findFirst()
            if (pageIndex != null) {
                pageIndex.deleteFromRealm()
            }
        }
    }

    private fun cleanupAnnotationFiles() {
       //TODO cleanup annotations & fire event bus events.
    }

    private fun cleanupAttachmentFiles() {
        //TODO cleanup attachments & fire event bus events.
    }
}

@RealmClass(embedded = true)
open class RItemField: RealmObject() {
    var key: String = ""
    var baseKey: String? = null
    var value: String = ""
    var changed: Boolean = false
}
