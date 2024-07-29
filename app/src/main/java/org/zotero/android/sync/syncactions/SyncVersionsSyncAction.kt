package org.zotero.android.sync.syncactions

import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.requests.MarkOtherObjectsAsChangedByUser
import org.zotero.android.database.requests.SyncVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.syncactions.architecture.SyncAction
import java.io.IOException

class SyncVersionsSyncAction(
    val objectS: SyncObject,
    val sinceVersion: Int?,
    val currentVersion: Int?,
    val syncType: SyncKind,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val syncDelayIntervals: List<Double>,
    val checkRemote: Boolean,
) : SyncAction() {

    suspend fun result(): Pair<Int, List<String>> {
        when (this@SyncVersionsSyncAction.objectS) {
            SyncObject.collection ->
                return synchronizeVersions(
                    libraryId = this@SyncVersionsSyncAction.libraryId,
                    userId = this@SyncVersionsSyncAction.userId,
                    objectS = this@SyncVersionsSyncAction.objectS,
                    sinceVersion = this@SyncVersionsSyncAction.sinceVersion,
                    currentVersion = this@SyncVersionsSyncAction.currentVersion,
                    syncType = this@SyncVersionsSyncAction.syncType
                )

            SyncObject.item -> {
                return synchronizeVersions(
                    libraryId = this@SyncVersionsSyncAction.libraryId,
                    userId = this@SyncVersionsSyncAction.userId,
                    objectS = this@SyncVersionsSyncAction.objectS,
                    sinceVersion = this@SyncVersionsSyncAction.sinceVersion,
                    currentVersion = this@SyncVersionsSyncAction.currentVersion,
                    syncType = this@SyncVersionsSyncAction.syncType
                )
            }

            SyncObject.trash -> {
                return synchronizeVersions(
                    libraryId = this@SyncVersionsSyncAction.libraryId,
                    userId = this@SyncVersionsSyncAction.userId,
                    objectS = this@SyncVersionsSyncAction.objectS,
                    sinceVersion = this@SyncVersionsSyncAction.sinceVersion,
                    currentVersion = this@SyncVersionsSyncAction.currentVersion,
                    syncType = this@SyncVersionsSyncAction.syncType
                )
            }

            SyncObject.search -> {
                return synchronizeVersions(
                    libraryId = this@SyncVersionsSyncAction.libraryId,
                    userId = this@SyncVersionsSyncAction.userId,
                    objectS = this@SyncVersionsSyncAction.objectS,
                    sinceVersion = this@SyncVersionsSyncAction.sinceVersion,
                    currentVersion = this@SyncVersionsSyncAction.currentVersion,
                    syncType = this@SyncVersionsSyncAction.syncType
                )
            }

            SyncObject.settings -> {
                return Pair(0, listOf())
            }
        }
    }

    private suspend fun synchronizeVersions(
        libraryId: LibraryIdentifier, userId: Long,
        objectS: SyncObject, sinceVersion: Int?,
        currentVersion: Int?, syncType: SyncKind
    ): Pair<Int, List<String>> {
        if (!this.checkRemote && this.syncType != SyncKind.full) {
            return loadChangedObjects(
                objectS = objectS,
                response = mapOf(),
                libraryId = libraryId,
                syncType = syncType,
                newVersion = (currentVersion ?: 0),
                delayIntervals = this.syncDelayIntervals
            )
        }

        val networkResult = loadRemoteVersions(
            objectS = objectS,
            libraryId = libraryId,
            userId = userId,
            sinceVersion = sinceVersion,
        )
        if (networkResult is CustomResult.GeneralSuccess.NetworkSuccess) {
            val decoded = networkResult.value
            val newVersion = networkResult.lastModifiedVersion
            val current = currentVersion
            if (current != null && newVersion != current) {
                throw SyncError.NonFatal.versionMismatch(libraryId)
            }
            return loadChangedObjects(
                objectS = objectS,
                response = decoded ?: emptyMap(),
                libraryId = libraryId,
                syncType = syncType,
                newVersion = newVersion,
                delayIntervals = this.syncDelayIntervals
            )
        }
        throw IOException((networkResult as CustomResult.GeneralError.NetworkError).stringResponse)
    }

    private suspend fun loadRemoteVersions(
        objectS: SyncObject,
        libraryId: LibraryIdentifier,
        userId: Long,
        sinceVersion: Int?,
    )
            : CustomResult<Map<String, Int>> {
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = userId) + "/" + objectS.apiPath

        val networkResult = safeApiCall {
            syncApi.versions(url, since = sinceVersion)
        }


        return networkResult
    }

    private fun loadChangedObjects(
        objectS: SyncObject,
        response: Map<String, Int>,
        libraryId: LibraryIdentifier,
        syncType: SyncKind,
        newVersion: Int,
        delayIntervals: List<Double>
    ): Pair<Int, List<String>> {
        var identifiers = mutableListOf<String>()
        dbWrapperMain.realmDbStorage.perform(coordinatorAction = { coordinator ->
            when (syncType) {
                SyncKind.full -> {
                    coordinator.perform(
                        request = MarkOtherObjectsAsChangedByUser(
                            syncObject = objectS,
                            versions = response,
                            libraryId = libraryId
                        )
                    )
                }

                SyncKind.collectionsOnly, SyncKind.ignoreIndividualDelays, SyncKind.normal,SyncKind.keysOnly, SyncKind.prioritizeDownloads -> {
                    //no-op
                }
            }

            val request = SyncVersionsDbRequest(
                versions = response,
                syncObject = objectS,
                syncType = syncType,
                delayIntervals = delayIntervals
            )
            identifiers = coordinator.perform(request = request).toMutableList()

            coordinator.invalidate()
        })
        return Pair(newVersion, identifiers)
    }
}