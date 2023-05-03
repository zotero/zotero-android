package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.sync.LibraryIdentifier

class ReadDocumentDataDbRequest(
    private val attachmentKey: String,
    private val libraryId: LibraryIdentifier,
): DbResponseRequest<Int> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): Int {
        val pageIndex =
            database.where<RPageIndex>().key(this.attachmentKey, this.libraryId).findFirst()
                ?: return 0
        return pageIndex.index
    }
}