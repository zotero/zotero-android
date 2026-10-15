package org.zotero.android.screens.reader.readaloud.voiceselection

import org.zotero.android.speech.SpeechVoice

data class ReadAloudVoiceChange(
    val voice: SpeechVoice,
    val preferredLanguage: String?,
)

data class ReadAloudVoiceSelectionResult(
    val change: ReadAloudVoiceChange?,
)