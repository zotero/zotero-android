package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadLibrariesDataDbRequest
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.LibraryData

class LoadLibraryDataSyncAction @AssistedInject constructor(
    @Assisted("type") private val type: Libraries,
    @Assisted("fetchUpdates") private val fetchUpdates: Boolean,
    @Assisted("loadVersions") private val loadVersions: Boolean,
    @Assisted("webDavEnabled") private val webDavEnabled: Boolean,

    private val dbWrapperMain: DbWrapperMain,
) {

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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("type") type: Libraries,
            @Assisted("fetchUpdates") fetchUpdates: Boolean,
            @Assisted("loadVersions") loadVersions: Boolean,
            @Assisted("webDavEnabled") webDavEnabled: Boolean
        ): LoadLibraryDataSyncAction
    }

}
