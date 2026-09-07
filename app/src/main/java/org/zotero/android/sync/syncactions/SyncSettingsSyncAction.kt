package org.zotero.android.sync.syncactions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.mappers.SettingsResponseMapper
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.StoreSettingsDbRequest
import org.zotero.android.sync.LibraryIdentifier

import org.zotero.android.sync.SyncError

class SyncSettingsSyncAction @AssistedInject constructor(
    @Assisted("currentVersion") private val currentVersion: Int?,
    @Assisted("sinceVersion") private val sinceVersion: Int,
    @Assisted("libraryId") private val libraryId: LibraryIdentifier,
    @Assisted("userId") private val userId: Long,

    private val zoteroApi: ZoteroApi,
    private val settingsResponseMapper: SettingsResponseMapper,
    private val dbWrapperMain: DbWrapperMain,
) {
    suspend fun result(): CustomResult<Pair<Boolean, Int>> {
        val url =
            BuildConfig.BASE_API_URL + "/" + libraryId.apiPath(userId = this.userId) + "/settings"

        val networkResult = safeApiCall {
            zoteroApi.settingsRequest(
                url = url,
                since = sinceVersion,
                headers = mapOf("If-Modified-Since-Version" to this.sinceVersion.toString())
            )
        }

        if (networkResult !is CustomResult.GeneralSuccess.NetworkSuccess) {
            return networkResult as CustomResult.GeneralError
        }
        val newVersion = networkResult.lastModifiedVersion
        val current = this.currentVersion
        if (current != null && current != newVersion) {
            return CustomResult.GeneralError.CodeError(SyncError.NonFatal.versionMismatch(this.libraryId))
        }
        val value = networkResult.value!!
        val response = settingsResponseMapper.fromJson(value)
        val request = StoreSettingsDbRequest(response = response, libraryId = this.libraryId)
        dbWrapperMain.realmDbStorage.perform(request = request)
        val settingsChanged = newVersion != this.sinceVersion
        return CustomResult.GeneralSuccess(Pair(settingsChanged, newVersion))
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("currentVersion") currentVersion: Int?,
            @Assisted("sinceVersion") sinceVersion: Int,
            @Assisted("libraryId") libraryId: LibraryIdentifier,
            @Assisted("userId") userId: Long
        ): SyncSettingsSyncAction
    }
}