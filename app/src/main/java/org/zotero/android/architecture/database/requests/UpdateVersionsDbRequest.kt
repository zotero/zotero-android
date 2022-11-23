package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.architecture.database.objects.RVersions
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject

sealed class UpdateVersionType {
    data class objectS(val syncObject: SyncObject) : UpdateVersionType()
    object deletions : UpdateVersionType()
}

class UpdateVersionsDbRequest(
    val version: Int,
    val libraryId: LibraryIdentifier,
    val type: UpdateVersionType,
) : DbRequest {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        when (this.libraryId) {
            is LibraryIdentifier.custom -> {
                val library = database
                    .where<RCustomLibrary>()
                    .equalTo("type", libraryId.type.name)
                    .findFirst() ?: throw DbError.objectNotFound
                val versions = library.versions
                if (versions != null) {
                    update(
                        versions = versions,
                        type = this.type,
                        version = this.version
                    )
                }
            }
            is LibraryIdentifier.group -> {
                val library = database
                    .where<RGroup>()
                    .equalTo("identifier", libraryId.groupId)
                    .findFirst() ?: throw DbError.objectNotFound
                val versions = library.versions
                if (versions != null) {
                    update(
                        versions = versions,
                        type = this.type,
                        version = this.version
                    )
                }
            }
        }
    }

    private fun update(
        versions: RVersions,
        type: UpdateVersionType,
        version: Int
    ) {
        when (type) {
            UpdateVersionType.deletions ->
                versions.deletions = version
            is UpdateVersionType.objectS -> {
                when (type.syncObject) {
                    SyncObject.collection ->
                        versions.collections = version
                    SyncObject.item ->
                        versions.items = version
                    SyncObject.trash ->
                        versions.trash = version
                    SyncObject.search ->
                        versions.searches = version
                    SyncObject.settings ->
                        versions.settings = version
                }
            }
        }
    }
}