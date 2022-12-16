package org.zotero.android.sync.syncactions

import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.database.RealmDbStorage
import org.zotero.android.architecture.database.requests.ReadLibrariesDataDbRequest
import org.zotero.android.sync.LibraryData
import org.zotero.android.sync.LibrarySyncType
import org.zotero.android.sync.SyncAction

class LoadLibraryDataSyncAction(
    val type: LibrarySyncType,
    val fetchUpdates: Boolean,
    val loadVersions: Boolean,
    val webDavEnabled: Boolean,
    val dbStorage: RealmDbStorage,
    val defaults: Defaults
) : SyncAction<List<LibraryData>> {

    override suspend fun result(): List<LibraryData> {
        val request =
            when (this.type) {
                LibrarySyncType.all -> {
                    ReadLibrariesDataDbRequest(
                        identifiers = null,
                        fetchUpdates = this.fetchUpdates,
                        loadVersions = this.loadVersions,
                        webDavEnabled = this.webDavEnabled,
                        defaults = defaults
                    )
                }
                is LibrarySyncType.specific -> {
                    if (this.type.identifiers.isEmpty()) {
                        return emptyList()
                    }
                    ReadLibrariesDataDbRequest(
                        identifiers = this.type.identifiers,
                        fetchUpdates = this.fetchUpdates,
                        loadVersions = this.loadVersions,
                        webDavEnabled = this.webDavEnabled,
                        defaults = defaults
                    )
                }
            }

        return dbStorage.perform(request = request, invalidateRealm = true)
    }

}
