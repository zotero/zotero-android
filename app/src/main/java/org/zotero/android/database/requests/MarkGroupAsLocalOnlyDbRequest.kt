package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RGroup
import org.zotero.android.sync.LibraryIdentifier

class MarkGroupAsLocalOnlyDbRequest(val groupId: Int) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val group =
            database
                .where<RGroup>()
                .equalTo("identifier", this.groupId).findFirst() ?: return

        group.isLocalOnly = true
        group.canEditFiles = false
        group.canEditMetadata = false
        MarkAllLibraryObjectChangesAsSyncedDbRequest(
            libraryId = LibraryIdentifier.group(this.groupId)
        ).process(database)
    }
}