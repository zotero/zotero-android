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
import org.zotero.android.sync.Versions
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

        var customLibraries = database.where<RCustomLibrary>()
        val types = separatedIds?.first
        if (types != null) {
            customLibraries = customLibraries.`in`("type", types.map{ it.name }.toTypedArray())
        }

        val customData = customLibraries.findAll().map { library ->
            val libraryId = LibraryIdentifier.custom(RCustomLibraryType.valueOf(library.type))
            val versions =
                if (this.loadVersions) Versions.init(versions = library.versions) else Versions.empty
            val version = versions.max
            val (updates, hasUpload) = updates(libraryId, version = version, database = database)
            val deletions = deletions(libraryId, version = version, database = database)
            val hasWebDavDeletions =
                if (!this.webDavEnabled) false else !database.where<RWebDavDeletion>().findAll()
                    .isEmpty()
            LibraryData(
                identifier = libraryId,
                name = RCustomLibraryType.valueOf(library.type).libraryName,
                versions = versions,
                canEditMetadata = true,
                canEditFiles = true,
                updates = updates,
                deletions = deletions,
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
            val versions =
                if (this.loadVersions) Versions.init(versions = group.versions) else Versions.empty
            val version = versions.max
            val (updates, hasUpload) = updates(libraryId, version = version, database = database)
            val deletions = deletions(libraryId, version = versions.max, database = database)
            LibraryData(
                identifier = libraryId,
                name = group.name,
                versions = versions,
                canEditMetadata = group.canEditMetadata,
                canEditFiles = group.canEditFiles,
                updates = updates,
                deletions = deletions,
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

    fun deletions(libraryId: LibraryIdentifier, version: Int, database: Realm): List<DeleteBatch> {
        if (!fetchUpdates) {
            return emptyList()
        }
        val collectionDeletions =
            ReadDeletedObjectsDbRequest<RCollection>(libraryId = libraryId).process(
                database,
                RCollection::class
            )
                .map { it.key }
                .chunked(DeleteBatch.maxCount)
                .map {
                    DeleteBatch(
                        libraryId = libraryId, objectS =
                        SyncObject.collection, version = version, keys = it
                    )
                }

        val searchDeletions = ReadDeletedObjectsDbRequest<RSearch>(libraryId = libraryId).process(
            database,
            RSearch::class
        )
            .map { it.key }
            .chunked(DeleteBatch.maxCount)
            .map {
                DeleteBatch(
                    libraryId = libraryId, objectS =
                    SyncObject.search, version = version, keys = it
                )
            }

        val itemDeletions = ReadDeletedObjectsDbRequest<RItem>(libraryId = libraryId).process(
            database,
            RItem::class
        )
            .map { it.key }
            .chunked(DeleteBatch.maxCount)
            .map {
                DeleteBatch(
                    libraryId = libraryId, objectS =
                    SyncObject.item, version = version, keys = it
                )
            }

        return collectionDeletions + searchDeletions + itemDeletions
    }

    private fun updates(libraryId: LibraryIdentifier, version: Int, database: Realm) : Pair<List<WriteBatch>, Boolean> {
        if (!fetchUpdates) {
            return emptyList<WriteBatch>() to false
        }

        val collectionParams =
            ReadUpdatedCollectionUpdateParametersDbRequest(libraryId = libraryId).process(database)
        val (itemParams, hasUpload) = ReadUpdatedItemUpdateParametersDbRequest(libraryId = libraryId).process(
            database
        )
        val searchParams =
            ReadUpdatedSearchUpdateParametersDbRequest(libraryId = libraryId).process(database)
        val settings =
            ReadUpdatedSettingsUpdateParametersDbRequest(libraryId = libraryId).process(database)

        val batches = writeBatches(
            collectionParams, libraryId = libraryId, version = version,
            SyncObject.collection
        ) +
                writeBatches(
                    itemParams, libraryId = libraryId, version = version,
                    SyncObject.item
                ) +
                writeBatches(
                    searchParams, libraryId = libraryId, version = version,
                    SyncObject.search
                ) +
                writeBatches(
                    settings, libraryId = libraryId, version = version,
                    SyncObject.settings
                )

        return batches to hasUpload
    }


    private fun writeBatches(response: ReadUpdatedParametersResponse, libraryId: LibraryIdentifier, version: Int, objectS: SyncObject): List<WriteBatch> {
        val chunks = response.parameters.chunked(WriteBatch.maxCount)
        var batches = mutableListOf<WriteBatch>()

        for (chunk in chunks) {
            var uuids = mutableMapOf<String, List<String>>()
            for (params in chunk) {
                val key = params["key"] as? String
                if (key != null) {
                    val _uuids = response.changeUuids[key]
                    if (_uuids != null) {
                        uuids[key] = _uuids
                    }
                }
            }
            batches.add(WriteBatch(libraryId =  libraryId, objectS = objectS, version = version, parameters =  chunk, changeUuids =  uuids))
        }

        return batches
    }

}
