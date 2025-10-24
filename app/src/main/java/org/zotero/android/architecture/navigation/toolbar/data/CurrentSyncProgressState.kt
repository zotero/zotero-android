package org.zotero.android.architecture.navigation.toolbar.data

sealed class CurrentSyncProgressState(open val message: String) {
    data class SyncFinishedWithError(override val message: String) : CurrentSyncProgressState(message)
    data class Aborted(override val message: String) : CurrentSyncProgressState(message)
    data class RegularMessage(override val message: String) : CurrentSyncProgressState(message)
}