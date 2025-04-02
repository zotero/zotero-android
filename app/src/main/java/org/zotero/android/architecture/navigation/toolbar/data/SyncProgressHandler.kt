package org.zotero.android.architecture.navigation.toolbar.data

import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import timber.log.Timber
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
        Timber.d("SyncProgressHandler: reportNewSync")
        cleanup()
        logCurrentProgress()
        emitState(SyncProgress.starting)
    }

    fun reportGroupsSync() {
        Timber.d("SyncProgressHandler: reportGroupsSync")
        emitState(SyncProgress.groups(null))
    }

    fun reportGroupCount(count: Int) {
        Timber.d("SyncProgressHandler: reportGroupCount $count")
        this.currentTotal = count
        this.currentDone = 0
        logCurrentProgress()
        reportGroupProgress()
    }

    fun reportGroupSynced() {
        Timber.d("SyncProgressHandler: reportGroupSynced")
        addDone(1)
        logCurrentProgress()
        reportGroupProgress()
    }
    fun reportLibrarySync(libraryId: LibraryIdentifier) {
        Timber.d("SyncProgressHandler: reportLibrarySync")
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(SyncProgress.library(name))
    }

    fun reportObjectSync(objectS: SyncObject, libraryId: LibraryIdentifier) {
        Timber.d("SyncProgressHandler: reportObjectSync")
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
        Timber.d("SyncProgressHandler: reportDownloadCount $count")
        this.currentTotal = count
        this.currentDone = 0
        logCurrentProgress()
        this.reportDownloadObjectProgress(objectS, libraryId = libraryId)
    }

    fun reportWrite(count: Int) {
        Timber.d("SyncProgressHandler: reportWrite $count")
        this.currentDone = 0
        this.currentTotal = count
        logCurrentProgress()
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
        Timber.d("SyncProgressHandler: reportDownloadBatchSynced $size")
        addDone(size)
        logCurrentProgress()
        reportDownloadObjectProgress(objectS, libraryId)
    }

    fun reportWriteBatchSynced(size: Int) {
        Timber.d("SyncProgressHandler: reportWriteBatchSynced $size")
        addDone(size)
        logCurrentProgress()
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
        Timber.d("SyncProgressHandler: reportUpload $count")
        this.currentTotal = count
        this.currentDone = 0
        logCurrentProgress()
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
        Timber.d("SyncProgressHandler: reportUploaded")
        addDone(1)
        logCurrentProgress()
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
        Timber.d("SyncProgressHandler: reportDeletions")
        val name = this.libraryNames?.get(libraryId) ?: return
        emitState(SyncProgress.deletions(name))
    }

    fun reportFinish(errors: List<SyncError.NonFatal>) {
        Timber.d("SyncProgressHandler: reportFinish errors ${errors.size}")
        finish(SyncProgress.finished(errors))
    }

    fun reportAbort(error: SyncError.Fatal) {
        Timber.d("SyncProgressHandler: reportAbort ${error.message}")
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
        logCurrentProgress()
    }

    private fun logCurrentProgress() {
        Timber.d("SyncProgressHandler: currentProgress $currentDone/$currentTotal")
    }

    fun muteProgressToolbarForScreen() {
        Timber.d("SyncProgressHandler: muteProgressToolbarForScreen")
        emitState(SyncProgress.shouldMuteWhileOnScreen(true))
    }

    fun unMuteProgressToolbarForScreen() {
        Timber.d("SyncProgressHandler: unMuteProgressToolbarForScreen")
        emitState(SyncProgress.shouldMuteWhileOnScreen(false))
    }

}