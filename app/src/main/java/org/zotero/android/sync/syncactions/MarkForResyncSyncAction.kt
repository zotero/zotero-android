package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.requests.MarkForResyncDbAction
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject

class MarkForResyncSyncAction @AssistedInject constructor(
    @Assisted("keys") private val keys: List<String>,
    @Assisted("objectS") private val objectS: SyncObject,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,

    private val dbWrapperMain: DbWrapperMain,
) {
    fun result() {
        val request = when (this.objectS) {
            SyncObject.collection ->
                MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RCollection::class)

            SyncObject.item, SyncObject.trash ->
                MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RItem::class)

            SyncObject.search ->
                MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RSearch::class)

            SyncObject.settings ->
                MarkForResyncDbAction(libraryId = this.libraryId, keys = this.keys, clazz = RPageIndex::class)
        }
        dbWrapperMain.realmDbStorage.perform(request = request)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("keys") keys: List<String>,
            @Assisted("objectS") objectS: SyncObject,
            @Assisted("libraryId") libraryId: LibraryIdentifier
        ): MarkForResyncSyncAction
    }

}