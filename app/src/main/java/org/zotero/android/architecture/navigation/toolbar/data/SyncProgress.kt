package org.zotero.android.architecture.navigation.toolbar.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject

sealed interface SyncProgress {
    object starting: SyncProgress
    data class groups(val progress: SyncProgressData?): SyncProgress
    data class library(val name: String): SyncProgress
    data class objectS(
        val objectS: SyncObject,
        val progress: SyncProgressData?,
        val libraryName: String,
        val libraryId: LibraryIdentifier
    ): SyncProgress
    data class deletions(val name: String): SyncProgress
    data class changes(val progress: SyncProgressData): SyncProgress
    data class uploads(val progress: SyncProgressData): SyncProgress
    data class finished(val errors: List<SyncError.NonFatal>): SyncProgress
    data class aborted(val error: SyncError.Fatal): SyncProgress
    data class shouldMuteWhileOnScreen(val shouldMute: Boolean): SyncProgress

    data class scanBarcodeMessage(val text: String): SyncProgress

}

data class SyncProgressData(
    val completed: Int,
    val total: Int
)