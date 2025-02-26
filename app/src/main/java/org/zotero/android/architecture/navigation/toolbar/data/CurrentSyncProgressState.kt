package org.zotero.android.architecture.navigation.toolbar.data

sealed class CurrentSyncProgressState(open val message: String) {
    data class SyncFinished(override val message: String, val hasErrors: Boolean) :
        CurrentSyncProgressState(message)

    data class InProgress(override val message: String) : CurrentSyncProgressState(message)
    data class Aborted(override val message: String) : CurrentSyncProgressState(message)
}