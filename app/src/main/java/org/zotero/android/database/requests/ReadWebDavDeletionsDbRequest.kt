package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RWebDavDeletion
import org.zotero.android.sync.LibraryIdentifier

class ReadWebDavDeletionsDbRequest(private val libraryId: LibraryIdentifier) :
    DbResponseRequest<RealmResults<RWebDavDeletion>> {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): RealmResults<RWebDavDeletion> {
        return database.where<RWebDavDeletion>().library(this.libraryId).findAll()
    }
}