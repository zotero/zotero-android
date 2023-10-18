package org.zotero.android.screens.addnote.data

import com.google.gson.annotations.SerializedName

data class WebViewUpdateMessage(
    @SerializedName("action") val action: String,
    @SerializedName("value") val value: String?,
)