package org.zotero.android.sync

import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.architecture.database.objects.RGroup

class LibraryData(
    val identifier: LibraryIdentifier,
    val name: String,
    val versions: Versions,
    val canEditMetadata: Boolean,
    val canEditFiles: Boolean,
    val updates: List<WriteBatch>,
    val deletions: List<DeleteBatch>,
    val hasUpload: Boolean,
    val hasWebDavDeletions: Boolean
) {

    companion object {
        private fun updates(chunkedParams: Map<SyncObject, List<List<Map<String, Any>>>>, version: Int, libraryId: LibraryIdentifier): List<WriteBatch> {
            var batches = mutableListOf<WriteBatch>()

            fun appendBatch(objectS: SyncObject) {
                val params = chunkedParams[objectS] ?: return
                batches.addAll(params.map {
                    WriteBatch(
                        libraryId = libraryId,
                        objectS = objectS,
                        version = version,
                        parameters = it
                    )
                })
            }


            appendBatch(SyncObject.collection)
            appendBatch(SyncObject.search)
            appendBatch(SyncObject.item)
            appendBatch(SyncObject.settings)

            return batches
        }

        private fun deletions(chunkedKeys: Map<SyncObject, List<List<String>>>, version: Int, libraryId: LibraryIdentifier): List<DeleteBatch> {
            var batches = mutableListOf<DeleteBatch>()

            fun appendBatch (objectS: SyncObject) {
                val keys = chunkedKeys[objectS] ?: return
                batches.addAll(keys.map {
                    DeleteBatch(
                        libraryId = libraryId,
                        objectS = objectS,
                        version = version,
                        keys = it
                    )
                })
            }

            appendBatch(SyncObject.collection)
            appendBatch(SyncObject.search)
            appendBatch(SyncObject.item)

            return batches
        }

        fun init(
            objectS: RCustomLibrary,
            loadVersions: Boolean,
            userId: Long,
            chunkedUpdateParams: Map<SyncObject, List<List<Map<String, Any>>>>,
            chunkedDeletionKeys: Map<SyncObject, List<List<String>>>,
            hasUpload: Boolean,
            hasWebDavDeletions: Boolean
        ): LibraryData {
            val type = RCustomLibraryType.valueOf(objectS.type)
            val versions =
                if (loadVersions) Versions.init(versions = objectS.versions) else Versions.empty
            val maxVersion = versions.max

            return LibraryData(
                identifier = LibraryIdentifier.custom(type),
                name = type.libraryName,
                versions = versions,
                canEditMetadata = true,
                canEditFiles = true,
                hasUpload = hasUpload,
                updates = LibraryData.updates(chunkedParams =  chunkedUpdateParams, version = maxVersion, libraryId = LibraryIdentifier.custom(type)),
                deletions = LibraryData.deletions(chunkedKeys = chunkedDeletionKeys, version =  maxVersion, libraryId=  LibraryIdentifier.custom(type)),
                hasWebDavDeletions = hasWebDavDeletions
            )
        }

        fun init(
            objectS: RGroup,
            loadVersions: Boolean,
            chunkedUpdateParams: Map<SyncObject, List<List<Map<String, Any>>>>,
            chunkedDeletionKeys: Map<SyncObject, List<List<String>>>,
            hasUpload: Boolean,
            hasWebDavDeletions: Boolean
        ): LibraryData {
            val versions =
                if (loadVersions) Versions.init(versions = objectS.versions) else Versions.empty
            val maxVersion = versions.max

            return LibraryData(
                identifier = LibraryIdentifier.group(objectS.identifier),
                name = objectS.name,
                versions = versions,
                canEditMetadata = objectS.canEditMetadata,
                canEditFiles = objectS.canEditFiles,
                hasUpload = hasUpload,
                updates = LibraryData.updates(
                    chunkedParams = chunkedUpdateParams,
                    version = maxVersion,
                    libraryId = LibraryIdentifier.group(objectS.identifier)
                ),
                deletions = LibraryData.deletions(
                    chunkedKeys = chunkedDeletionKeys,
                    version = maxVersion,
                    libraryId = LibraryIdentifier.group(objectS.identifier)
                ),
                hasWebDavDeletions = hasWebDavDeletions
            )
        }
    }



}
