import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.requests.ReadLibrariesDataDbRequest
import org.zotero.android.sync.LibrarySyncType

class LoadLibraryDataSyncAction(
    val type: LibrarySyncType,
    val fetchUpdates: Boolean,
    val loadVersions: Boolean,
    val webDavEnabled: Boolean,
    val dbWrapper: DbWrapper,
) {

    var result: Single<[LibraryData]> {
        return Single.create { subscriber -> Disposable in
            let request: ReadLibrariesDataDbRequest

            switch self.type {
            case .all:
                request = ReadLibrariesDataDbRequest(identifiers: nil, fetchUpdates: self. fetchUpdates, loadVersions: self.loadVersions, webDavEnabled: self.webDavEnabled)
            case .specific(let ids):
                if ids.isEmpty {
                    subscriber(.success([]))
                    return Disposables.create()
                }
                request = ReadLibrariesDataDbRequest(identifiers: ids, fetchUpdates: self. fetchUpdates, loadVersions: self.loadVersions, webDavEnabled: self.webDavEnabled)
            }

            do {
                let data = try self.dbStorage.perform(request: request, on: self.queue, invalidateRealm: true)
                subscriber(.success(data))
            } catch let error {
                subscriber(.failure(error))
            }

            return Disposables.create()
        }
    }
}
