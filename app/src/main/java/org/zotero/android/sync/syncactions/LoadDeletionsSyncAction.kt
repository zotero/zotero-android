package org.zotero.android.sync.syncactions

import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.LibraryIdentifier

import org.zotero.android.sync.SyncError
import org.zotero.android.sync.syncactions.architecture.SyncAction

data class LoadDeletionsSyncActionResult(
    val collections: List<String>,
    val items: List<String>,
    val searches: List<String>,
    val tags: List<String>,
    val version: Int
)

class LoadDeletionsSyncAction(
    private val currentVersion: Int?,
    private val sinceVersion: Int,
    private val libraryId: LibraryIdentifier,
    private val userId: Long,
): SyncAction() {

    suspend fun result(): CustomResult<LoadDeletionsSyncActionResult> {
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = this.userId) + "/deleted"

        val networkResult = safeApiCall {
            zoteroApi.deletionRequest(
                url = url,
                since = sinceVersion,
                headers = mapOf("If-Modified-Since-Version" to this.sinceVersion.toString())
            )
        }

        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        val newVersion = networkResult.lastModifiedVersion
        val version = this.currentVersion
        if (version != null && version != newVersion) {
            return CustomResult.GeneralError.CodeError(SyncError.NonFatal.versionMismatch(this.libraryId))
        }
        val value = networkResult.value!!
        return CustomResult.GeneralSuccess(
            LoadDeletionsSyncActionResult(
                collections = value.collections,
                items = value.items,
                searches = value.searches,
                tags = value.tags,
                version = newVersion
            )
        )
    }
}