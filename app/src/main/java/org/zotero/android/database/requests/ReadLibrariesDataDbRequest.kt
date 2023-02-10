package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.Defaults
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.RWebDavDeletion
import org.zotero.android.sync.DeleteBatch
import org.zotero.android.sync.LibraryData
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.Versions
import org.zotero.android.sync.WriteBatch

class ReadLibrariesDataDbRequest(
    private val identifiers: List<LibraryIdentifier>?,
    private val fetchUpdates: Boolean,
    private val loadVersions: Boolean,
    private val webDavEnabled: Boolean,
    private val defaults: Defaults
) : DbResponseRequest<List<LibraryData>> {

    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): List<LibraryData> {
        val allLibraryData = mutableListOf<LibraryData>()

        val userId = defaults.getUserId()
        val separatedIds = if (identifiers == null) {
            null
        } else {
            separateTypes(identifiers)
        }

        var customLibraries = database.where<RCustomLibrary>()
        val types = separatedIds?.first
        if (types != null) {
            customLibraries = customLibraries
                .`in`("type", types.map { it.name }
                    .toTypedArray())
        }

        val customData = customLibraries.findAll().map { library ->
            val libraryId = LibraryIdentifier.custom(RCustomLibraryType.valueOf(library.type))
            val versions =
                if (this.loadVersions) {
                    Versions.init(versions = library.versions)
                } else {
                    Versions.empty
                }
            val version = versions.max
            val (updates, hasUpload) = updates(
                libraryId = libraryId,
                version = version,
                database = database
            )
            val deletions = deletions(
                libraryId = libraryId,
                version = version,
                database = database
            )
            val hasWebDavDeletions =
                if (!this.webDavEnabled) {
                    false
                } else {
                    !database
                        .where<RWebDavDeletion>()
                        .findAll()
                        .isEmpty()
                }
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

        var groups = database
            .where<RGroup>()
            .equalTo("isLocalOnly", false)
        val groupIds = separatedIds?.second
        if (groupIds != null) {
            groups = groups.`in`("identifier", groupIds.toTypedArray())
        }
        groups = groups.sort("name")
        val groupData = groups.findAll().map { group ->
            val libraryId = LibraryIdentifier.group(group.identifier)
            val versions =
                if (this.loadVersions) {
                    Versions.init(versions = group.versions)
                } else {
                    Versions.empty
                }
            val version = versions.max
            val (updates, hasUpload) = updates(
                libraryId = libraryId,
                version = version,
                database = database
            )
            val deletions =
                deletions(
                    libraryId = libraryId,
                    version = versions.max,
                    database = database
                )
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

    private fun separateTypes(identifiers: List<LibraryIdentifier>)
            : Pair<List<RCustomLibraryType>, List<Int>> {
        val custom = mutableListOf<RCustomLibraryType>()
        val group = mutableListOf<Int>()
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

    fun deletions(
        libraryId: LibraryIdentifier,
        version: Int,
        database: Realm
    ): List<DeleteBatch> {
        if (!fetchUpdates) {
            return emptyList()
        }
        val collectionDeletions =
            ReadDeletedObjectsDbRequest(
                libraryId = libraryId, clazz = RCollection::class
            ).process(
                database = database,
            )
            .map { it.key }
            .chunked(DeleteBatch.maxCount)
            .map {
                DeleteBatch(
                    libraryId = libraryId,
                    objectS = SyncObject.collection,
                    version = version,
                    keys = it
                )
            }

        val searchDeletions = ReadDeletedObjectsDbRequest<RSearch>(libraryId = libraryId, clazz = RSearch::class)
            .process(
                database = database,
            )
            .map { it.key }
            .chunked(DeleteBatch.maxCount)
            .map {
                DeleteBatch(
                    libraryId = libraryId,
                    objectS = SyncObject.search,
                    version = version,
                    keys = it
                )
            }

        val itemDeletions = ReadDeletedObjectsDbRequest(
            libraryId = libraryId, clazz = RItem::class
        )
            .process(
                database = database,
            )
            .map { it.key }
            .chunked(DeleteBatch.maxCount)
            .map {
                DeleteBatch(
                    libraryId = libraryId,
                    objectS = SyncObject.item,
                    version = version,
                    keys = it
                )
            }
        return collectionDeletions + searchDeletions + itemDeletions
    }

    private fun updates(
        libraryId: LibraryIdentifier,
        version: Int,
        database: Realm
    ): Pair<List<WriteBatch>, Boolean> {
        if (!fetchUpdates) {
            return emptyList<WriteBatch>() to false
        }

        val collectionParams =
            ReadUpdatedCollectionUpdateParametersDbRequest(libraryId = libraryId).process(database)
        val (itemParams, hasUpload) = ReadUpdatedItemUpdateParametersDbRequest(libraryId = libraryId)
            .process(database)
        val searchParams =
            ReadUpdatedSearchUpdateParametersDbRequest(libraryId = libraryId).process(database)
        val settings =
            ReadUpdatedSettingsUpdateParametersDbRequest(libraryId = libraryId).process(database)

        val batches = writeBatches(
            response = collectionParams,
            libraryId = libraryId,
            version = version,
            objectS = SyncObject.collection
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


    private fun writeBatches(
        response: ReadUpdatedParametersResponse,
        libraryId: LibraryIdentifier,
        version: Int,
        objectS: SyncObject
    ): List<WriteBatch> {
        val chunks = response.parameters.chunked(WriteBatch.maxCount)
        val batches = mutableListOf<WriteBatch>()

        for (chunk in chunks) {
            val uuids = mutableMapOf<String, List<String>>()
            for (params in chunk) {
                val key = params["key"] as? String
                if (key != null) {
                    val _uuids = response.changeUuids[key]
                    if (_uuids != null) {
                        uuids[key] = _uuids
                    }
                }
            }
            batches.add(
                WriteBatch(
                    libraryId = libraryId,
                    objectS = objectS,
                    version = version,
                    parameters = chunk,
                    changeUuids = uuids
                )
            )
        }

        return batches
    }
}
