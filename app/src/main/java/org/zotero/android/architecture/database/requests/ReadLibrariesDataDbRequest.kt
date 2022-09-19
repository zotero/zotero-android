package org.zotero.android.architecture.database.requests
import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.architecture.database.objects.RWebDavDeletion
import org.zotero.android.sync.DeleteBatch
import org.zotero.android.sync.LibraryData
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.WriteBatch
import kotlin.reflect.KClass

class ReadLibrariesDataDbRequest(
    private val identifiers: List<LibraryIdentifier>?,
    private val fetchUpdates: Boolean,
    private val loadVersions: Boolean,
    private val webDavEnabled: Boolean,
    private val sdkPrefs: SdkPrefs
) : DbResponseRequest<List<LibraryData>, List<LibraryData>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm, clazz: KClass<List<LibraryData>>?): List<LibraryData> {
        var allLibraryData = mutableListOf<LibraryData>()

        val userId = sdkPrefs.getUserId()
        val separatedIds = if (identifiers == null) null else separateTypes(identifiers)

        var customLibraries = database.where<RCustomLibrary>().findAll().toList()
        val types = separatedIds?.first
        if (types != null) {
            customLibraries = customLibraries.filter { rCustomLibrary ->
                types.map { it.name }.contains(rCustomLibrary.type)
            }
        }

        val customData = customLibraries.map { library ->
            val libraryId = LibraryIdentifier.custom(RCustomLibraryType.valueOf(library.type))
            val (updates, hasUpload) = updates(libraryId, database)
            val deletions = deletions(libraryId, database)
            val hasWebDavDeletions =
                if (!this.webDavEnabled) false else !database.where<RWebDavDeletion>().findAll()
                    .isEmpty()
            LibraryData.init(
                objectS = library,
                loadVersions = this.loadVersions,
                userId = userId,
                chunkedUpdateParams = updates,
                chunkedDeletionKeys = deletions,
                hasUpload = hasUpload,
                hasWebDavDeletions = hasWebDavDeletions
            )
        }

        customData.forEach {
            allLibraryData.add(it)
        }

        var groups = database.where<RGroup>().equalTo("isLocalOnly", false)
        val groupIds = separatedIds?.second
        if (groupIds != null) {
            groups = groups.`in`("identifier", groupIds.toTypedArray())
        }
        groups = groups.sort("name")
        val groupData = groups.findAll().map { group ->
            val libraryId = LibraryIdentifier.group(group.identifier)
            val (updates, hasUpload) = updates(libraryId, database)
            LibraryData.init(
                objectS = group,
                loadVersions = this.loadVersions,
                chunkedUpdateParams = updates,
                chunkedDeletionKeys = deletions(libraryId, database),
                hasUpload = hasUpload,
                hasWebDavDeletions = false
            )
        }

        groupData.forEach {
            allLibraryData.add(it)

        }

        return allLibraryData
    }

    private fun separateTypes(identifiers: List<LibraryIdentifier>): Pair<List<RCustomLibraryType>,  List<Int>> {
        var custom = mutableListOf<RCustomLibraryType>()
        var group = mutableListOf<Int>()
        identifiers.forEach { identifier ->
            when (identifier) {
                is LibraryIdentifier.custom ->
                    custom.add(identifier.type)
                is LibraryIdentifier.group ->
                    group.add(identifier.groupId)
            }
        }
        return Pair(custom, group)
    }

    fun deletions(libraryId: LibraryIdentifier, database: Realm): Map<SyncObject, List<List<String>>> {
        if (!fetchUpdates) {
            return emptyMap()
        }
        val chunkSize = DeleteBatch.maxCount
        return mapOf(SyncObject.collection to ReadDeletedObjectsDbRequest<RCollection>(libraryId = libraryId).process(database, RCollection::class).map { it.key }
            .chunked(chunkSize),
            SyncObject.search to ReadDeletedObjectsDbRequest<RSearch>(libraryId = libraryId).process(database, RSearch::class).map { it.key }
            .chunked(chunkSize),
            SyncObject.item to ReadDeletedObjectsDbRequest<RItem>(libraryId = libraryId).process(database,RItem::class).map {it.key}.chunked(chunkSize))
    }

    private fun updates(libraryId: LibraryIdentifier, database: Realm):  Pair<Map<SyncObject, List<List<Map<String, Any>>>>, Boolean> {
        if (!this.fetchUpdates) {
            return emptyMap<SyncObject, List<List<Map<String, Any>>>>() to false
        }
        val chunkSize = WriteBatch.maxCount
        val (itemParams, hasUpload) = ReadUpdatedItemUpdateParametersDbRequest(libraryId = libraryId).process(
            database
        )
        val settings =
            ReadUpdatedSettingsUpdateParametersDbRequest(libraryId = libraryId).process(database)
        return mapOf(
            SyncObject.collection to ReadUpdatedCollectionUpdateParametersDbRequest(libraryId = libraryId).process(
                database
            ).chunked(chunkSize),
            SyncObject.search to ReadUpdatedSearchUpdateParametersDbRequest(libraryId = libraryId).process(
                database
            ).chunked(chunkSize),
            SyncObject.item to itemParams.chunked(chunkSize),
            SyncObject.settings to (if (settings.isEmpty()) emptyList() else listOf(settings))
        ) to hasUpload
    }

}
