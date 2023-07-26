package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RTypedTag
import org.zotero.android.sync.LibraryIdentifier

class ReadAutomaticTagsDbRequest(
    private val libraryId: LibraryIdentifier,
) : DbResponseRequest<RealmResults<RTypedTag>> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RealmResults<RTypedTag> {
        return database.where<RTypedTag>().typedTagLibrary(this.libraryId)
            .rawPredicate("type = \"${RTypedTag.Kind.automatic.name}\"").findAll()
    }
}