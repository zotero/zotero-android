package org.zotero.android.speech

import android.speech.tts.Voice
import org.zotero.android.speech.data.RemoteVoice

sealed class SpeechVoice {
    data class Local(val voice: Voice) : SpeechVoice()
    data class Remote(val voice: RemoteVoice) : SpeechVoice()
}