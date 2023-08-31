package org.zotero.android.architecture.navigation.toolbar.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import java.lang.Integer.min
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncProgressHandler @Inject constructor(
    private val syncProgressEventStream: SyncProgressEventStream,
) {

    private var libraryNames: Map<LibraryIdentifier, String>? = null
    private var currentDone: Int = 0
    private var currentTotal: Int = 0

    fun set(libraryNames: Map<LibraryIdentifier, String>) {
        this.libraryNames = libraryNames
    }

    fun reportNewSync() {
        cleanup()
        emitState(SyncProgress.starting)
    }

    fun reportGroupsSync() {
        emitState(SyncProgress.groups(null))
    }

    fun reportGroupCount(count: Int) {
        this.currentTotal = count
        this.currentDone = 0
        reportGroupProgress()
    }

    fun reportGroupSynced() {
        addDone(1)
        reportGroupProgress()
    }
    fun reportLibrarySync(libraryId: LibraryIdentifier) {
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(SyncProgress.library(name))
    }

    fun reportObjectSync(objectS: SyncObject, libraryId: LibraryIdentifier) {
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(
            SyncProgress.objectS(
                objectS = objectS,
                progress = null,
                libraryName = name,
                libraryId = libraryId
            )
        )
    }

    fun reportDownloadCount(objectS: SyncObject, count: Int, libraryId: LibraryIdentifier) {
        this.currentTotal = count
        this.currentDone = 0
        this.reportDownloadObjectProgress(objectS, libraryId = libraryId)
    }

    fun reportWrite(count: Int) {
        this.currentDone = 0
        this.currentTotal = count
        emitState(
            SyncProgress.changes(
                progress = SyncProgressData(
                    this.currentDone,
                    this.currentTotal
                )
            )
        )
    }

    fun reportDownloadBatchSynced(size: Int, objectS: SyncObject, libraryId: LibraryIdentifier) {
        addDone(size)
        reportDownloadObjectProgress(objectS, libraryId)
    }

    fun reportWriteBatchSynced(size: Int) {
        addDone(size)
        emitState(
            SyncProgress.changes(
                progress = SyncProgressData(
                    this.currentDone,
                    this.currentTotal
                )
            )
        )
    }
    fun reportUpload(count: Int) {
        this.currentTotal = count
        this.currentDone = 0
        emitState(
            SyncProgress.uploads(
                progress = SyncProgressData(
                    this.currentDone,
                    this.currentTotal
                )
            )
        )
    }

    fun reportUploaded() {
        addDone(1)
        emitState(
            SyncProgress.uploads(
                progress = SyncProgressData(
                    this.currentDone,
                    this.currentTotal
                )
            )
        )
    }

    fun reportDeletions(libraryId: LibraryIdentifier) {
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(SyncProgress.deletions(name))
    }

    fun reportFinish(errors: List<SyncError.NonFatal>) {
        finish(SyncProgress.finished(errors))
    }

    fun reportAbort(error: SyncError.Fatal) {
        finish(SyncProgress.aborted(error))
    }

    private fun addDone(done: Int) {
        this.currentDone = min((this.currentDone + done), this.currentTotal)
    }

    private fun reportGroupProgress() {
        emitState(SyncProgress.groups(SyncProgressData(this.currentDone, this.currentTotal)))
    }

    private fun reportDownloadObjectProgress(objectS: SyncObject, libraryId: LibraryIdentifier) {
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(
            SyncProgress.objectS(
                objectS = objectS,
                progress = SyncProgressData(this.currentDone, this.currentTotal),
                libraryName = name,
                libraryId = libraryId
            )
        )
    }

    private fun finish(state: SyncProgress) {
        cleanup()
        emitState(state)
    }

    private fun emitState(state: SyncProgress) {
        syncProgressEventStream.emitAsync(state)
    }

    private fun cleanup() {
        this.libraryNames = null
        this.currentDone = 0
        this.currentTotal = 0
    }


}