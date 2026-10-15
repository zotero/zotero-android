package org.zotero.android.speech

import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.speech.VoicesResponse
import org.zotero.android.speech.data.RemoteVoice
import timber.log.Timber

class RemoteVoiceResolver(
    private val remoteVoicesController: RemoteVoicesController,
    private val voiceUtility: VoiceUtility,
) {
    sealed class Error : Exception() {
        object missingVoices : Error()
        data class loadFailed(val error: CustomResult.GeneralError) : Error()
    }

    private var allAvailableVoices: VoicesResponse? = null

    suspend fun loadAvailableVoices(): VoicesResponse {
        allAvailableVoices?.let { return it }
        val result = remoteVoicesController.loadVoices()
        if (result is CustomResult.GeneralError) {
            Timber.e("RemoteVoiceResolver: can't load voices - $result")
            throw Error.loadFailed(result)
        }
        val voicesResult = (result as CustomResult.GeneralSuccess).value ?: throw Error.loadFailed(
            CustomResult.GeneralError.CodeError(RemoteVoicesController.Error.noData)
        )
        allAvailableVoices = voicesResult.response
        return voicesResult.response
    }

    suspend fun standardVoice(language: String): RemoteVoice? {
        return voiceUtility.findRemoteVoice(
            language = language,
            tier = RemoteVoice.Tier.standard,
            response = loadAvailableVoices(),
        )
    }

    suspend fun resolveVoice(language: String, tier: RemoteVoice.Tier): RemoteVoice {
        val allVoices = loadAvailableVoices()
        return voiceUtility.findRemoteVoice(language = language, tier = tier, response = allVoices)
            ?: allVoices.firstVoice(tier)
            ?: throw Error.missingVoices
    }
}