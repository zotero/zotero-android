package org.zotero.android.sync.syncactions

import org.zotero.android.architecture.SdkPrefs
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
    val sdkPrefs: SdkPrefs
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
                        sdkPrefs = sdkPrefs
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
                        sdkPrefs = sdkPrefs
                    )
                }
            }

        return dbStorage.perform(request = request, invalidateRealm = true)
    }

}
