package org.zotero.android.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalVoiceCatalog @Inject constructor(
    private val context: Context,
) {
    private val mutex = Mutex()

    @Volatile
    private var isLoaded = false

    @Volatile
    var voices: List<Voice> = emptyList()
        private set

    suspend fun loadVoices(forceReload: Boolean = false): List<Voice> = mutex.withLock {
        if (isLoaded && !forceReload) {
            return voices
        }
        val engine = createEngine() ?: return voices
        try {
            voices = engine.voices.orEmpty().filter { it.isInstalledOnDevice }
            isLoaded = true
        } catch (error: Exception) {
            Timber.e(error, "LocalVoiceCatalog: can't load voices")
        } finally {
            engine.shutdown()
        }
        return voices
    }

    suspend fun createEngine(): TextToSpeech? {
        return suspendCancellableCoroutine { continuation ->
            val holder = arrayOfNulls<TextToSpeech>(1)
            val engine = TextToSpeech(context) { status ->
                val created = holder[0]
                if (status == TextToSpeech.SUCCESS && created != null) {
                    continuation.resume(created)
                } else {
                    Timber.e("LocalVoiceCatalog: TextToSpeech init failed - $status")
                    created?.shutdown()
                    continuation.resume(null)
                }
            }
            holder[0] = engine
            continuation.invokeOnCancellation { engine.shutdown() }
        }
    }

    private val Voice.isInstalledOnDevice: Boolean
        get() = !isNetworkConnectionRequired && !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
}