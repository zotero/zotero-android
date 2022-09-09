package org.zotero.android.architecture.database.requests
import DbResponseRequest
import io.realm.Realm
import io.realm.kotlin.Realm
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.sync.DeleteBatch
import org.zotero.android.sync.LibraryData
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject

class ReadLibrariesDataDbRequest(
    private val identifiers: List<LibraryIdentifier>?,
    private val fetchUpdates: Boolean,
    private val loadVersions: Boolean,
    private val webDavEnabled: Boolean,
    private val sdkPrefs: SdkPrefs
) : DbResponseRequest<List<LibraryData>> {

    override val needsWrite: Boolean
        get() = false

    fun process(database: Realm) : List<LibraryData> {
        var allLibraryData = mutableListOf<LibraryData>()

        val userId = sdkPrefs.getUserId()
        val separatedIds = identifiers.flatMap { separateTypes(in: $0) }

        var customLibraries = database.objects(RCustomLibrary.self)
        if let types = separatedIds?.custom {
            customLibraries = customLibraries.filter("type IN %@", types.map({ $0.rawValue }))
        }
        let customData = try customLibraries.map({ library -> LibraryData in
            let libraryId = LibraryIdentifier.custom(library.type)
            let (updates, hasUpload) = try self.updates(for: libraryId, database: database)
            let deletions = try self.deletions(for: libraryId, database: database)
            let hasWebDavDeletions = !self.webDavEnabled ? false : !database.objects(RWebDavDeletion.self).isEmpty
            return LibraryData(object: library, loadVersions: self. loadVersions, userId: userId, chunkedUpdateParams: updates, chunkedDeletionKeys: deletions, hasUpload: hasUpload,
                               hasWebDavDeletions: hasWebDavDeletions)
        })
        allLibraryData.append(contentsOf: customData)

        var groups = database.objects(RGroup.self).filter("isLocalOnly = false")
        if let groupIds = separatedIds?.group {
            groups = groups.filter("identifier IN %@", groupIds)
        }
        groups = groups.sorted(byKeyPath: "name")
        let groupData = try groups.map({ group -> LibraryData in
            let libraryId = LibraryIdentifier.group(group.identifier)
            let (updates, hasUpload) = try self.updates(for: libraryId, database: database)
            return LibraryData(object: LibraryIdentifier.group, loadVersions: self. loadVersions, chunkedUpdateParams: updates, chunkedDeletionKeys: try self.deletions(for: libraryId, database: database),
                               hasUpload: hasUpload, hasWebDavDeletions: false)
        })
        allLibraryData.append(contentsOf: groupData)

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
        return SyncObject.collection: try ReadDeletedObjectsDbRequest<RCollection>(libraryId: libraryId).process(in: database).map({ $0.key }).chunked(into: chunkSize),
                .SyncObject.search: try ReadDeletedObjectsDbRequest<RSearch>(libraryId: libraryId).process(in: database).map({ $0.key }).chunked(into: chunkSize),
                .SyncObject.item: try ReadDeletedObjectsDbRequest<RItem>(libraryId: libraryId).process(in: database).map({ $0.key }).chunked(into: chunkSize)]
    }

    private func updates(for libraryId: LibraryIdentifier, database: Realm) throws -> ([SyncObject: [[[String: Any]]]], Bool) {
        guard self.fetchUpdates else { return ([:], false) }
        let chunkSize = WriteBatch.maxCount
        let (itemParams, hasUpload) = try ReadUpdatedItemUpdateParametersDbRequest(libraryId: libraryId).process(in: database)
        let settings = try ReadUpdatedSettingsUpdateParametersDbRequest(libraryId: libraryId).process(in: database)
        return (SyncObject.collection: try ReadUpdatedCollectionUpdateParametersDbRequest(libraryId: libraryId).process(in: database).chunked(into: chunkSize),
                 .SyncObject.search: try ReadUpdatedSearchUpdateParametersDbRequest(libraryId: libraryId).process(in: database).chunked(into: chunkSize),
                 .SyncObject.item: itemParams.chunked(into: chunkSize),
                 .SyncObject.settings: (settings.isEmpty ? [] : [settings])],
                hasUpload)
    }
}
