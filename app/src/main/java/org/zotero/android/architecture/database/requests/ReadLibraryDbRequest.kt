package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbResponseRequest
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier

class ReadLibraryDbRequest(
    val libraryId: LibraryIdentifier
) : DbResponseRequest<Library> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Library {
        when (this.libraryId) {
            is LibraryIdentifier.custom -> return Library(
                identifier = this.libraryId,
                name = this.libraryId.type.libraryName,
                metadataEditable = true,
                filesEditable = true
            )
            is LibraryIdentifier.group -> {
                val group = database
                    .where<RGroup>()
                    .equalTo("identifier", this.libraryId.groupId)
                    .findFirst() ?: throw DbError.objectNotFound
                return Library(
                    identifier = this.libraryId,
                    name = group.name,
                    metadataEditable = group.canEditMetadata,
                    filesEditable = group.canEditFiles
                )
            }
        }
    }
}