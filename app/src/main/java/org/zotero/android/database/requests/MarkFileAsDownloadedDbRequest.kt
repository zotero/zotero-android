package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber

class MarkFileAsDownloadedDbRequest(
    private val key: String,
    private val libraryId: LibraryIdentifier,
    private val downloaded: Boolean,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val item = database.where<RItem>().key(this.key, this.libraryId).findFirst()
        if (item == null) {
            Timber.e("MarkFileAsDownloadedDbRequest: item not found")
            return
        }
        if (item.rawType == ItemTypes.attachment && item.fileDownloaded != this.downloaded) {
            item.fileDownloaded = this.downloaded
        }
    }

}