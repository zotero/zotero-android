package org.zotero.android.sync

import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.StoreLastReadDateDbRequest
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastReadWatcher @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
) {
    private var lastUpdate: Pair<String, LibraryIdentifier>? = null
    private var pendingUpdate: Triple<String, LibraryIdentifier, Date?>? = null
    private var timer: BackgroundTimer? = null

    fun didEnterBackground() {
        flushPendingAndStop(sync = true)
    }

    fun willTerminate() {
        flushPendingAndStop(sync = true)
    }

    private fun flushPendingAndStop(sync: Boolean = false) {
        if (pendingUpdate != null) {
            store(
                key = pendingUpdate!!.first,
                libraryId = pendingUpdate!!.second,
                date = pendingUpdate!!.third,
                sync = sync
            )
        }
        pendingUpdate = null
        lastUpdate = null
        timer = null
    }

    fun submit(key: String, libraryId: LibraryIdentifier, date: Date?) {
        if (pendingUpdate != null && timer?.state == BackgroundTimer.State.resumed) {
            store(
                key = pendingUpdate!!.first,
                libraryId = pendingUpdate!!.second,
                date = pendingUpdate!!.third
            )
        }
        store(key = key, libraryId = libraryId, date = date)
        lastUpdate = key to libraryId
        pendingUpdate = null
        timer = null
    }

    fun submitAfterDelay(key: String, libraryId: LibraryIdentifier, date: Date?) {
        if (lastUpdate?.first == key && lastUpdate?.second == libraryId) {
            pendingUpdate = Triple(key, libraryId, date)

            if (timer != null && timer?.state != BackgroundTimer.State.suspended) {
                return
            }

            val timer = BackgroundTimer(300 * 1000L) {
                flushPendingAndStop()
            }
            this.timer = timer
            timer.resume()
        } else {
            if (pendingUpdate != null) {
                store(
                    key = pendingUpdate!!.first,
                    libraryId = pendingUpdate!!.second,
                    date = pendingUpdate!!.third
                )
            }
            store(key = key, libraryId = libraryId, date = date)
            lastUpdate = Pair(key, libraryId)
            pendingUpdate = null
            timer = null
        }
    }

    private fun store(
        key: String,
        libraryId: LibraryIdentifier,
        date: Date?,
        sync: Boolean = false
    ) {
        try {
            dbWrapperMain.realmDbStorage.perform(
                StoreLastReadDateDbRequest(
                    key = key,
                    libraryId = libraryId,
                    date = date
                )
            )
        } catch (error: Exception) {
            Timber.e(error, "LastReadWatcher: can't store last read date")
        }
    }
}