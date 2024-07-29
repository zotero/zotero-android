package org.zotero.android.sync.syncactions

import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.requests.MarkForResyncDbAction
import org.zotero.android.sync.LibraryIdentifier

import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction

class MarkForResyncSyncAction(
    val keys: List<String>,
    val objectS: SyncObject,
    val libraryId: LibraryIdentifier,
): SyncAction() {
    fun result() {
        val request: DbRequest
        when (this.objectS) {
            SyncObject.collection ->
                request = MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RCollection::class)
            SyncObject.item, SyncObject.trash ->
                request = MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RItem::class)
            SyncObject.search ->
                request = MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RSearch::class)
            SyncObject.settings ->
                request = MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RPageIndex::class)
        }
        dbWrapperMain.realmDbStorage.perform(request = request)
    }
}