package org.zotero.android.appupdate

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.BuildConfig
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateSuggestionUseCase @Inject constructor(
    private val context: Context,
    private val nonZoteroApi: NonZoteroApi,
    private val dispatcher: CoroutineDispatcher
) {

    private val versionRegex = "(\\d{1,3}).(\\d{1,3}).(\\d{1,3})-(\\d{1,4})".toRegex()

    private val googlePlayValidInstallers = setOf(
        "com.android.vending",
        "com.google.android.feedback"
    )

    fun wasDownloadedFromGooglePlayStore(): Boolean {
        val installer =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).initiatingPackageName
            } else {
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        return installer != null && googlePlayValidInstallers.contains(installer)
    }


    suspend fun getNewestAppVersionFromManifest() = withContext(dispatcher) {
        val result = safeApiCall {
            nonZoteroApi.getAppUpdateManifest()
        }
        if (result is CustomResult.GeneralError.NetworkError) {
            return@withContext null
        }
        if (result is CustomResult.GeneralError.CodeError) {
            Timber.e(result.throwable)
            return@withContext null
        }

        if (result is CustomResult.GeneralSuccess.NetworkSuccess) {
            val productionNodeObject =
                result.value?.get("channels")?.asJsonObject?.get("production")?.asJsonObject
            val versionString =
                productionNodeObject?.get("version")?.asString ?: return@withContext null
            return@withContext versionString
        }
        return@withContext null
    }

    fun shouldShowUpdateAppDialog(versionFromManifest: String?): Boolean {
        if (versionFromManifest == null) {
            return false
        }
        versionRegex.matchEntire(versionFromManifest)
            ?.destructured
            ?.let { (manifestMajor, manifestMinor, manifestPatch, manifestVersionCode) ->
                val shouldUpdate = manifestMajor.toInt() > BuildConfig.BUILD_VERSION_MAJOR
                        || manifestMinor.toInt() > BuildConfig.BUILD_VERSION_MINOR
                        || manifestPatch.toInt() > BuildConfig.BUILD_VERSION_PATCH
                        || manifestVersionCode.toInt() > BuildConfig.VERSION_CODE
                return shouldUpdate
            }
        return false
    }
}