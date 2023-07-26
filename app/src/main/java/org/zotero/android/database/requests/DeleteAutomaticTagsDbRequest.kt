package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RItemChanges
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier
import java.util.Date

class DeleteAutomaticTagsDbRequest(
    private val libraryId: LibraryIdentifier,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val typedTags = ReadAutomaticTagsDbRequest(libraryId = this.libraryId).process(database)

        val date = Date()
        for (tag in typedTags) {
            val item = tag.item ?: continue
            item.changes.add(RObjectChange.create(changes = listOf(RItemChanges.tags)))
            item.dateModified = date
            tag.item = null
        }
        typedTags.deleteAllFromRealm()

        val tags = database.where<RTag>().library(this.libraryId)
            .rawPredicate("color = \"\"")
            .rawPredicate("tags.@count = 0").findAll()
        tags.deleteAllFromRealm()
    }
}