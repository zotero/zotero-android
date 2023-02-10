package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RGroup
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Versions

class ReadVersionDbRequest(val libraryId: LibraryIdentifier): DbResponseRequest<Int> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Int {
        when (this.libraryId) {
            is LibraryIdentifier.custom -> {
                val library =
                    database.where<RCustomLibrary>().equalTo("type", this.libraryId.type.name)
                        .findFirst()
                if (library == null) {
                    throw DbError.objectNotFound
                }
                return Versions.init(versions = library.versions).max
            }

            is LibraryIdentifier.group -> {
                val library = database.where<RGroup>().equalTo("identifier", this.libraryId.groupId).findFirst()
                if (library == null) {
                    throw DbError.objectNotFound
                }
                return Versions.init(versions = library.versions).max
            }
        }
    }
}