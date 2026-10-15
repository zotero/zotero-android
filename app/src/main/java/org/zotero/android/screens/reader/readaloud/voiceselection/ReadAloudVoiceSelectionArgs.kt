package org.zotero.android.screens.reader.readaloud.voiceselection

import org.zotero.android.speech.SpeechVoice

data class ReadAloudVoiceSelectionArgs(
    val initialVoice: SpeechVoice?,
    val language: String?,
    val detectedLanguage: String,
    val savesOnVoiceChange: Boolean,
)