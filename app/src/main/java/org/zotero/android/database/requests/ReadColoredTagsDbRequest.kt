package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier

class ReadColoredTagsDbRequest(val libraryId: LibraryIdentifier) :
    DbResponseRequest<RealmResults<RTag>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RTag> {
        return database.where<RTag>()
            .library(this.libraryId)
            .and()
            .isNotEmpty("color")
            .findAll()
    }
}