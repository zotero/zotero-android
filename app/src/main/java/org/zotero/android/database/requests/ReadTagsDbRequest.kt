package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag

class ReadTagsDbRequest(val libraryId: LibraryIdentifier) : DbResponseRequest<List<Tag>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): List<Tag> {
        return database
            .where<RTag>()
            .library(this.libraryId)
            .and()
            .beginGroup()
            .greaterThan("tags.@count", 0)
            .or()
            .notEqualTo("color", "")
            .endGroup()
            .sort("name")
            .findAll().map { Tag(it) }
    }
}