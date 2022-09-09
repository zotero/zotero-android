package org.zotero.android.architecture.database.objects
import androidx.compose.ui.text.toLowerCase
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.RealmClass
import org.zotero.android.architecture.database.objects.FieldKeys.Item.Companion.publicationTitle
import org.zotero.android.formatter.ItemTitleFormatter
import java.util.Date

class RItemChanges(val rawValue: Short) {
    companion object {
        val type = RItemChanges((1 shl 0).toShort())
        val trash = RItemChanges(1 shl 1)
        val parent = RItemChanges( 1 shl 2)
        val collections = RItemChanges(1 shl 3)
        val fields = RItemChanges( 1 shl 4)
        val tags = RItemChanges(1 shl 5)
        val creators = RItemChanges(1 shl 6)
        val relations = RItemChanges( 1 shl 7)
        val rects = RItemChanges(1 shl 8)
        val paths = RItemChanges( 1 shl 9)
        val all: List<RItemChanges> = listOf(RItemChanges.type, RItemChanges.trash, RItemChanges.parent, RItemChanges.collections, RItemChanges.fields, RItemChanges.tags, RItemChanges.creators, RItemChanges.relations)
    }


}


class RItem: RealmObject() {

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
    var key: String = ""
    var rawType: String = ""
    var baseTitle: String = ""
    var inPublications: Boolean = false
    lateinit var dateAdded: Date
    lateinit  var dateModified: Date
    var parent: RItem? = null
    var createdBy: RUser? = null
    var lastModifiedBy: RUser? = null
    var customLibraryKey: RCustomLibraryType? = null
    var groupKey: Int? = null
    @LinkingObjects("items")
    lateinit var collections: RealmList<RCollection>

    lateinit var fields: RealmList<RItemField>

    @LinkingObjects("parent")
    lateinit var children: RealmList<RItem>

    @LinkingObjects("item")
    lateinit var tags: RealmList<RTypedTag>

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
    var version: Int = 0
    var attachmentNeedsSync: Boolean = false
    lateinit var syncState: ObjectSyncState
    lateinit var lastSyncDate: Date
    var syncRetries: Int = 0
    var rawChangedFields: Short = 0
    lateinit var changeType: UpdatableChangeType
    var deleted: Boolean = false

    val doi: String? get() {
        val fieldS = fields.filter { it.key == FieldKeys.Item.doi }.first()
        val doi = FieldKeys.Item.clean(doi = fieldS.value)
        return if(!doi.isEmpty()) doi else null
    }

    val urlString: String? get() {
        return fields.filter{it.key == FieldKeys.Item.url}.firstOrNull()?.value
    }

    var changedFields: RItemChanges
        get() {
            return RItemChanges(rawChangedFields)
        }
        set(newValue) {
            rawChangedFields = newValue.rawValue
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
}

@RealmClass(embedded = true)
class RItemField: RealmObject() {
    var key: String = ""
    var baseKey: String? = null
    var value: String = ""
    var changed: Boolean = false
}
