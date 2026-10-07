package org.zotero.android.speech.data

import android.speech.tts.Voice

data class ReadAloudLanguage(
    val id: String,
    val name: String,
    val locales: List<String>,
)

data class LocaleRemoteVoiceGroup(
    val locale: String,
    val displayName: String,
    val voices: List<RemoteVoice>,
) {
    val id: String get() = locale
}

data class LocaleLocalVoiceGroup(
    val locale: String,
    val displayName: String,
    val voices: List<Voice>,
) {
    val id: String get() = locale
}