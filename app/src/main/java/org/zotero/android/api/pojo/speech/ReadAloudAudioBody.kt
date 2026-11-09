package org.zotero.android.api.pojo.speech

import com.google.gson.annotations.SerializedName

data class ReadAloudAudioBody(
    @SerializedName("text") val text: String,
    @SerializedName("voice") val voice: String,
)