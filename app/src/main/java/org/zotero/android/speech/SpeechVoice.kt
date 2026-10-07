package org.zotero.android.speech

import android.speech.tts.Voice
import org.zotero.android.speech.data.RemoteVoice

sealed class SpeechVoice {
    data class Local(val voice: Voice) : SpeechVoice()
    data class Remote(val voice: RemoteVoice) : SpeechVoice()
}

val Voice.identifier: String
    get() = name

val Voice.languageTag: String
    get() = locale.toLanguageTag()

val Voice.baseLanguage: String
    get() = LanguageDetector.baseLanguage(of = languageTag)

val Voice.qualitySortOrder: Int
    get() = when {
        quality >= Voice.QUALITY_VERY_HIGH -> 0
        quality >= Voice.QUALITY_HIGH -> 1
        else -> 2
    }