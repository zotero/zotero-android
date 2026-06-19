package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.sync.LibraryIdentifier

import org.zotero.android.sync.SyncError

data class LoadDeletionsSyncActionResult(
    val collections: List<String>,
    val items: List<String>,
    val searches: List<String>,
    val tags: List<String>,
    val version: Int
)

class LoadDeletionsSyncAction @AssistedInject constructor(
    @Assisted("currentVersion") private val currentVersion: Int?,
    @Assisted("sinceVersion") private val sinceVersion: Int,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("userId") private val userId: Long,

    private val zoteroApi: ZoteroApi,
) {

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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("currentVersion") currentVersion: Int?,
            @Assisted("sinceVersion") sinceVersion: Int,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("userId") userId: Long
        ): LoadDeletionsSyncAction
    }

}