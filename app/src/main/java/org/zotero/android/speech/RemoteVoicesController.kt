package org.zotero.android.speech

import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.CustomResult.GeneralSuccess.NetworkSuccess
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.api.pojo.speech.VoicesResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteVoicesController @Inject constructor(
    private val zoteroApi: ZoteroApi,
) {
    sealed class Error : Exception() {
        object noData : Error()
    }

    data class Credits(
        val standard: Int,
        val premium: Int,
    )

    data class VoicesResult(
        val response: VoicesResponse,
        val credits: Credits,
    )

    suspend fun loadVoices(): CustomResult<VoicesResult> {
        val networkResult = safeApiCall { zoteroApi.voicesRequest() }
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as NetworkSuccess
        val voicesResponse = networkResult.value ?: return CustomResult.GeneralError.CodeError(Error.noData)
        val standardCredits = networkResult.headers["zotero-tts-standard-credits-remaining"]?.toIntOrNull() ?: 0
        val premiumCredits = networkResult.headers["zotero-tts-premium-credits-remaining"]?.toIntOrNull() ?: 0
        return CustomResult.GeneralSuccess(
            VoicesResult(
                response = voicesResponse,
                credits = Credits(standard = standardCredits, premium = premiumCredits),
            )
        )
    }

    suspend fun loadCredits(): CustomResult<Credits> {
        val networkResult = safeApiCall { zoteroApi.creditsRequest() }
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as NetworkSuccess
        val response = networkResult.value ?: return CustomResult.GeneralError.CodeError(Error.noData)
        return CustomResult.GeneralSuccess(
            Credits(standard = response.standardCreditsRemaining, premium = response.premiumCreditsRemaining)
        )
    }

    suspend fun downloadSample(voiceId: String): CustomResult<ByteArray> {
        val networkResult = safeApiCall { zoteroApi.readAloudSampleRequest(voiceId = voiceId) }
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as NetworkSuccess
        val body = networkResult.value ?: return CustomResult.GeneralError.CodeError(Error.noData)
        return CustomResult.GeneralSuccess(body.bytes())
    }

    suspend fun downloadSound(text: String, voiceId: String): CustomResult<ByteArray> {
        val networkResult = safeApiCall { zoteroApi.readAloudAudioRequest(text = text, voiceId = voiceId) }
        if (networkResult is CustomResult.GeneralError) {
            return networkResult
        }
        networkResult as NetworkSuccess
        val body = networkResult.value ?: return CustomResult.GeneralError.CodeError(Error.noData)
        return CustomResult.GeneralSuccess(body.bytes())
    }
}
