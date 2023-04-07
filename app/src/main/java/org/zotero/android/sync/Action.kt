package org.zotero.android.sync;

import org.zotero.android.database.requests.PerformDeletionsDbRequest

sealed class Action {
    object loadKeyPermissions : Action()
    object syncGroupVersions : Action()
    data class createLibraryActions(
        val librarySyncType: LibrarySyncType,
        val createLibraryActionsOptions: CreateLibraryActionsOptions
    ) : Action()

    data class resolveDeletedGroup(
        val groupId: Int,
        val name: String
    ) : Action()

    data class syncGroupToDb(val a: Int) : Action()

    data class resolveGroupMetadataWritePermission(
        val groupId: Int,
        val libraryDataName: String
    ) : Action()

    data class performWebDavDeletions(override val libraryId: LibraryIdentifier) : Action()

    data class submitDeleteBatch(val batch: DeleteBatch) : Action()

    data class createUploadActions(
        override val libraryId: LibraryIdentifier,
        val hadOtherWriteActions: Boolean
    ) : Action()

    data class syncVersions(
        override val libraryId: LibraryIdentifier,
        val objectS: SyncObject,
        val version: Int,
        val checkRemote: Boolean
    ) : Action()

    data class submitWriteBatch(val batch: WriteBatch) : Action()

    data class uploadAttachment(val upload: AttachmentUpload) : Action()

    data class syncDeletions(
        override val libraryId: LibraryIdentifier,
        val version: Int
    ) : Action()

    data class storeDeletionVersion(
        override val libraryId: LibraryIdentifier,
        val version: Int
    ) : Action()

    data class syncBatchesToDb(val batches: List<DownloadBatch>) : Action()

    data class syncSettings(
        override val libraryId: LibraryIdentifier,
        val version: Int
    ) : Action()

    data class storeVersion(
        val version: Int,
        override val libraryId: LibraryIdentifier,
        val syncObject: SyncObject
    ) : Action()

    data class markChangesAsResolved(override val libraryId: LibraryIdentifier) : Action()

    data class markGroupAsLocalOnly(val groupId: Int) : Action()

    data class revertLibraryToOriginal(val libraryIdentifier: LibraryIdentifier): Action()

    data class deleteGroup(val groupId: Int) : Action()

    data class performDeletions(
        override val libraryId: LibraryIdentifier,
        val collections: List<String>,
        val items: List<String>,
        val searches: List<String>,
        val tags: List<String>,
        val conflictMode: PerformDeletionsDbRequest.ConflictResolutionMode
    ) : Action()

    data class restoreDeletions(
        val libraryIdentifier: LibraryIdentifier,
        val collections: List<String>,
        val items: List<String>
    ) : Action()

    open val libraryId: LibraryIdentifier?
        get() {
            val action = this
            return when (action) {
                is loadKeyPermissions, is createLibraryActions, is syncGroupVersions ->
                    return null
                is resolveDeletedGroup -> return LibraryIdentifier.group(action.groupId)
                is syncGroupToDb -> return LibraryIdentifier.group(action.a)
                is createUploadActions -> action.libraryId
                is performWebDavDeletions -> action.libraryId
                is resolveGroupMetadataWritePermission -> return LibraryIdentifier.group(action.groupId)
                is storeDeletionVersion -> action.libraryId
                is submitDeleteBatch -> action.batch.libraryId
                is submitWriteBatch -> action.batch.libraryId
                is syncDeletions -> action.libraryId
                is syncSettings -> action.libraryId
                is syncVersions -> action.libraryId
                is storeVersion -> action.libraryId
                is performDeletions -> action.libraryId
                is syncBatchesToDb -> action.batches.firstOrNull()?.libraryId
                is markChangesAsResolved -> action.libraryId
                is markGroupAsLocalOnly -> LibraryIdentifier.group(action.groupId)
                is deleteGroup -> LibraryIdentifier.group(action.groupId)
                is uploadAttachment -> action.upload.libraryId
                is restoreDeletions -> action.libraryIdentifier
                is revertLibraryToOriginal -> action.libraryIdentifier
            }
        }

    val requiresConflictReceiver: Boolean
        get() {
            return when (this) {
                is loadKeyPermissions, is createLibraryActions, is syncGroupVersions ->
                    return false
                is resolveDeletedGroup -> true
                is syncGroupToDb -> false
                is createUploadActions -> false
                is performWebDavDeletions -> false
                is resolveGroupMetadataWritePermission -> true
                is storeDeletionVersion -> false
                is submitDeleteBatch -> false
                is submitWriteBatch -> false
                is syncDeletions -> true
                is syncSettings -> false
                is syncVersions -> false
                is storeVersion -> false
                is syncBatchesToDb -> false
                is performDeletions -> false
                is markChangesAsResolved -> false
                is markGroupAsLocalOnly -> false
                is deleteGroup -> false
                is uploadAttachment -> false
                is restoreDeletions -> false
                is revertLibraryToOriginal -> false
            }
        }
}
