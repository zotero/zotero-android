package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.MarkOtherObjectsAsChangedByUser
import org.zotero.android.database.requests.SyncVersionsDbRequest
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObject
import java.io.IOException

class SyncVersionsSyncAction @AssistedInject constructor(
    @Assisted("objectS") private val objectS: SyncObject,
    @Assisted("sinceVersion") private val sinceVersion: Int?,
    @Assisted("currentVersion") private val currentVersion: Int?,
    @Assisted("syncType") private val syncType: SyncKind,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("userId") private val userId: Long,
    @Assisted("syncDelayIntervals") private val syncDelayIntervals: List<Double>,
    @Assisted("checkRemote") private val checkRemote: Boolean,

    private val zoteroApi: ZoteroApi,
    private val dbWrapperMain: DbWrapperMain,
) {

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
            zoteroApi.versions(url, since = sinceVersion)
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

                SyncKind.collectionsOnly, SyncKind.ignoreIndividualDelays, SyncKind.normal, SyncKind.keysOnly, SyncKind.prioritizeDownloads -> {
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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("objectS") objectS: SyncObject,
            @Assisted("sinceVersion") sinceVersion: Int?,
            @Assisted("currentVersion") currentVersion: Int?,
            @Assisted("syncType") syncType: SyncKind,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("userId") userId: Long,
            @Assisted("syncDelayIntervals") syncDelayIntervals: List<Double>,
            @Assisted("checkRemote") checkRemote: Boolean,
        ): SyncVersionsSyncAction
    }
}