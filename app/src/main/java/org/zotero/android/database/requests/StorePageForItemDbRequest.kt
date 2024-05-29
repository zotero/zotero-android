package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RObjectChange
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RPageIndexChanges
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.LibraryIdentifier

class StorePageForItemDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val page: String,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val pageIndex: RPageIndex
        val existing = database.where<RPageIndex>().key(this.key, this.libraryId).findFirst()
        if (existing != null) {
            if (existing.index == this.page) {
                return
            }
            pageIndex = existing
        } else {
            pageIndex = database.createObject<RPageIndex>()
            pageIndex.key = this.key
            pageIndex.libraryId = this.libraryId
        }

        pageIndex.index = this.page
        pageIndex.changes.add(RObjectChange.create(listOf(RPageIndexChanges.index)))
        pageIndex.changeType = UpdatableChangeType.user.name
    }
}