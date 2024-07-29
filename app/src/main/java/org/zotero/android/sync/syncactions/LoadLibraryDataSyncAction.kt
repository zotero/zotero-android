package org.zotero.android.sync.syncactions

import org.zotero.android.database.requests.ReadLibrariesDataDbRequest
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.LibraryData
import org.zotero.android.sync.syncactions.architecture.SyncAction


class LoadLibraryDataSyncAction(
    val type: Libraries,
    val fetchUpdates: Boolean,
    val loadVersions: Boolean,
    val webDavEnabled: Boolean,
) : SyncAction() {

    fun result(): List<LibraryData> {
        val request =
            when (this.type) {
                Libraries.all -> {
                    ReadLibrariesDataDbRequest(
                        identifiers = null,
                        fetchUpdates = this.fetchUpdates,
                        loadVersions = this.loadVersions,
                        webDavEnabled = this.webDavEnabled,
                    )
                }
                is Libraries.specific -> {
                    if (this.type.identifiers.isEmpty()) {
                        return emptyList()
                    }
                    ReadLibrariesDataDbRequest(
                        identifiers = this.type.identifiers,
                        fetchUpdates = this.fetchUpdates,
                        loadVersions = this.loadVersions,
                        webDavEnabled = this.webDavEnabled,
                    )
                }
            }

        return dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
    }

}
