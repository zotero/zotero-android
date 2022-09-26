package org.zotero.android.sync.syncactions

import androidx.compose.runtime.snapshots.Snapshot.Companion.observe
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.ItemTypes.Companion.case
import org.zotero.android.architecture.database.objects.RCollection
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.RSearch
import org.zotero.android.architecture.database.requests.MarkOtherObjectsAsChangedByUser
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncAction
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncObject
import org.zotero.android.sync.SyncType

class SyncVersionsSyncAction(
    val objectS: SyncObject,
    val sinceVersion: Int?,
    val currentVersion: Int?,
    val syncType: SyncType,
    val libraryId: LibraryIdentifier,
    val userId: Int,
    val syncDelayIntervals: List<Double>,
    val checkRemote: Boolean,
    val dbStorage: DbWrapper
) : SyncAction<Pair<Int, List<String>>> {

    override suspend fun result(): Pair<Int, List<String>> {
        when(this.objectS) {
            SyncObject.collection ->
            return self.synchronizeVersions(for: RCollection.self, libraryId: self.libraryId, userId: self.userId, object: self.object, since: self.sinceVersion, current: self.currentVersion,
            syncType: self.syncType)
            SyncObject.item
                :
            return self.synchronizeVersions(for: RItem.self, libraryId: self.libraryId, userId: self.userId, object: self.object, since: self.sinceVersion, current: self.currentVersion,
            syncType: self.syncType)
            case .trash:
            return self.synchronizeVersions(for: RItem.self, libraryId: self.libraryId, userId: self.userId, object: self.object, since: self.sinceVersion, current: self.currentVersion,
            syncType: self.syncType)
            SyncObject.search
                :
            return self.synchronizeVersions(for: RSearch.self, libraryId: self.libraryId, userId: self.userId, object: self.object, since: self.sinceVersion, current: self.currentVersion,
            syncType: self.syncType)
            SyncObject.settings
                :
            return Single.just((0, []))
        }
    }

    private func synchronizeVersions<Obj: SyncableObject & Deletable & Updatable>(for type: Obj.Type, libraryId: LibraryIdentifier, userId: Int, object: SyncObject, since sinceVersion: Int?,
                                                                                  current currentVersion: Int?, syncType: SyncController.SyncType) -> Single<(Int, [String])> {
        if !self.checkRemote && self.syncType != .full {
            return self.loadChangedObjects(for: object, from: [:], in: libraryId, syncType: syncType, newVersion: (currentVersion ?? 0), delayIntervals: self.syncDelayIntervals)
                       .observe(on: self.scheduler)
        }

        return self.loadRemoteVersions(for: object, in: libraryId, userId: userId, since: sinceVersion, syncType: syncType)
                   .observe(on: self.scheduler)
                   .flatMap { (decoded: [String: Int], response) -> Single<(Int, [String])> in
                       let newVersion = response.allHeaderFields.lastModifiedVersion

                       if let current = currentVersion, newVersion != current {
                           return Single.error(SyncError.NonFatal.versionMismatch(libraryId))
                       }

                       return self.loadChangedObjects(for: object, from: decoded, in: libraryId, syncType: syncType, newVersion: newVersion, delayIntervals: self.syncDelayIntervals)
                   }
    }

    private fun loadRemoteVersions(for object: SyncObject, in libraryId: LibraryIdentifier, userId: Int, since sinceVersion: Int?, syncType: SyncController.SyncType)
                                                                                                                                                           -> Single<([String: Int], HTTPURLResponse)> {
        let request = VersionsRequest(libraryId: libraryId, userId: userId, objectType: object, version: sinceVersion)
        return self.apiClient.send(request: request, queue: self.queue)
    }

    private suspend fun loadChangedObjects(
        objectS: SyncObject,
        response: Map<String, Int>,
        libraryId: LibraryIdentifier,
        syncType: SyncType,
        newVersion: Int,
        delayIntervals: List<Double>
    ):
                                                                                                                                                                            Pair<Int, List<String>> {
            do {
                var identifiers = mutableListOf<String>()
                dbStorage.realmDbStorage.perform(coordinatorAction = { coordinator ->
                    when (syncType) {
                        SyncType.full -> {
                            coordinator.perform(request = MarkOtherObjectsAsChangedByUser(syncObject =  objectS, versions = response, libraryId = libraryId))
                            }

                        SyncType.collectionsOnly, SyncType.ignoreIndividualDelays, SyncType.normal -> {}
                    }


                    val request = SyncVersionsDbRequest(versions = response, libraryId = libraryId, syncObject =  objectS, syncType = syncType, delayIntervals =  delayIntervals)
                    identifiers = coordinator.perform(request: request)

                        coordinator.invalidate()
                })

                try self.dbStorage.perform(on: self.queue, with: { coordinator in

                })

                subscriber(.success((newVersion, identifiers)))
            } catch let error {
                subscriber(.failure(error))
            }

            return Disposables.create()
    }
}
