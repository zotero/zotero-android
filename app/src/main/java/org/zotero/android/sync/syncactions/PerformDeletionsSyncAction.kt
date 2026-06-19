package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.PerformDeletionsDbRequest
import org.zotero.android.sync.LibraryIdentifier


class PerformDeletionsSyncAction @AssistedInject constructor(
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("collections") private val collections: List<String>,
    @Assisted("items") private val items: List<String>,
    @Assisted("searches") private val searches: List<String>,
    @Assisted("tags") private val tags: List<String>,
    @Assisted("conflictMode") private val conflictMode: PerformDeletionsDbRequest.ConflictResolutionMode,

    private val dbWrapperMain: DbWrapperMain,
) {
    fun result(): List<Pair<String, String>> {
        val request = PerformDeletionsDbRequest(
            libraryId = this.libraryId,
            collections = this.collections,
            items = this.items,
            searches = this.searches,
            tags = this.tags,
            conflictMode = this.conflictMode
        )
        val conflicts = dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
        return conflicts
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("collections") collections: List<String>,
            @Assisted("items") items: List<String>,
            @Assisted("searches") searches: List<String>,
            @Assisted("tags") tags: List<String>,
            @Assisted("conflictMode") conflictMode: PerformDeletionsDbRequest.ConflictResolutionMode
        ): PerformDeletionsSyncAction
    }

}