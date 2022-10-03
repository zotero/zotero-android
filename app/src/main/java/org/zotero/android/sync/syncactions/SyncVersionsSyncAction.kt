package org.zotero.android.sync.syncactions

import org.zotero.android.BuildConfig
import org.zotero.android.api.SyncApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.MarkOtherObjectsAsChangedByUser
import org.zotero.android.architecture.database.requests.SyncVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.SyncType
import java.io.IOException

class SyncVersionsSyncAction(
    val objectS: SyncObject,
    val sinceVersion: Int?,
    val currentVersion: Int?,
    val syncType: SyncType,
    val libraryId: LibraryIdentifier,
    val userId: Long,
    val syncDelayIntervals: List<Double>,
    val checkRemote: Boolean,
    val dbWrapper: DbWrapper,
    val syncApi: SyncApi,
) : SyncAction<Pair<Int, List<String>>> {

    override suspend fun result(): Pair<Int, List<String>> {
        when (this.objectS) {
            SyncObject.collection ->
                return synchronizeVersions(
                    libraryId = this.libraryId,
                    userId = this.userId, objectS = this.objectS, sinceVersion = this.sinceVersion,
                    currentVersion = this.currentVersion, syncType = this.syncType
                )
            SyncObject.item -> {
                return synchronizeVersions(
                    libraryId = this.libraryId,
                    userId = this.userId,
                    objectS = this.objectS,
                    sinceVersion = this.sinceVersion,
                    currentVersion = this.currentVersion,
                    syncType = this.syncType
                )
            }

            SyncObject.trash -> {
                return synchronizeVersions(
                    libraryId = this.libraryId,
                    userId = this.userId,
                    objectS = this.objectS,
                    sinceVersion = this.sinceVersion,
                    currentVersion = this.currentVersion,
                    syncType = this.syncType
                )
            }

            SyncObject.search -> {
                return synchronizeVersions(
                    libraryId = this.libraryId,
                    userId = this.userId,
                    objectS = this.objectS,
                    sinceVersion = this.sinceVersion,
                    currentVersion = this.currentVersion,
                    syncType = this.syncType
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
        currentVersion: Int?, syncType: SyncType
    ): Pair<Int, List<String>> {
        if (!this.checkRemote && this.syncType != SyncType.full) {
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
            syncType = syncType
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
        syncType: SyncType
    )
            : CustomResult<Map<String, Int>> {
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = userId) + "/" + objectS.apiPath

        val networkResult = safeApiCall {
            syncApi.versions(url, since = sinceVersion)
        }


        return networkResult
    }

    private suspend fun loadChangedObjects(
        objectS: SyncObject,
        response: Map<String, Int>,
        libraryId: LibraryIdentifier,
        syncType: SyncType,
        newVersion: Int,
        delayIntervals: List<Double>
    ): Pair<Int, List<String>> {
        var identifiers = mutableListOf<String>()
        dbWrapper.realmDbStorage.perform(coordinatorAction = { coordinator ->
            when (syncType) {
                SyncType.full -> {
                    coordinator.perform(
                        request = MarkOtherObjectsAsChangedByUser(
                            syncObject = objectS,
                            versions = response,
                            libraryId = libraryId
                        )
                    )
                }

                SyncType.collectionsOnly, SyncType.ignoreIndividualDelays, SyncType.normal -> {
                }
            }


            val request = SyncVersionsDbRequest(
                versions = response,
                libraryId = libraryId,
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