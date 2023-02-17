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

class MarkItemsFilesAsNotDownloadedDbRequest(
    private val keys: Set<String>,
    private val libraryId: LibraryIdentifier,
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val items = database.where<RItem>()
            .keys(this.keys, this.libraryId).and()
            .item(type = ItemTypes.attachment).and()
            .file(downloaded = true).findAll()
        for (item in items) {
            if (item.attachmentNeedsSync) {
                continue
            }
            item.fileDownloaded = false
        }
    }
}

class MarkLibraryFilesAsNotDownloadedDbRequest(
    private val libraryId: LibraryIdentifier
): DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val items =
            database.where<RItem>()
                .library(this.libraryId).and()
                .item(type = ItemTypes.attachment).and()
                .file(downloaded = true).findAll()
        for (item in items) {
            if (item.attachmentNeedsSync) {
                continue
            }
            item.fileDownloaded = false
        }
    }

}

class MarkAllFilesAsNotDownloadedDbRequest: DbRequest {

    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val items = database.where<RItem>().item(type = ItemTypes.attachment).and().file(downloaded = true).findAll()
        for (item in items) {
            if (item.attachmentNeedsSync) {
                continue
            }
            item.fileDownloaded = false
        }
    }
}