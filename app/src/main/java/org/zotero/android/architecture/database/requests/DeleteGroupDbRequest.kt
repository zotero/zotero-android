package org.zotero.android.architecture.database.requests

import io.realm.Realm
import io.realm.RealmQuery
import io.realm.kotlin.where
import org.zotero.android.architecture.database.DbRequest
import org.zotero.android.architecture.database.objects.Deletable
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.architecture.database.objects.RTag
import org.zotero.android.sync.LibraryIdentifier

class DeleteGroupDbRequest(val groupId: Int) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val libraryId = LibraryIdentifier.group(this.groupId)
        deleteObjects(
            database
                .where<RItem>()
                .library(libraryId), database
        )
        deleteObjects(
            database
                .where<RCollection>()
                .library(libraryId), database
        )
        deleteObjects(
            database
                .where<RSearch>()
                .library(libraryId), database
        )

        val tags = database
            .where<RTag>()
            .library(libraryId).findAll()
        for (tag in tags) {
            if (tag.isInvalidated) {
                continue
            }
            tag.tags?.deleteAllFromRealm()
        }
        tags.deleteAllFromRealm()

        val objectS = database
            .where<RGroup>()
            .equalTo("identifier", this.groupId)
            .findFirst()

        if (objectS != null) {
            if (objectS.isInvalidated) {
                return
            }

            objectS.deleteFromRealm()
        }

    }

    private fun <T : Deletable> deleteObjects(query: RealmQuery<T>, database: Realm) {
        val objects = query.findAll()
        for (objectS in objects) {
            if (objectS.isInvalidated) {
                continue
            }
            objectS.willRemove(database)
        }
        objects.deleteAllFromRealm()
    }
}