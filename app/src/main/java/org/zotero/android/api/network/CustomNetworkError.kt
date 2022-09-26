package org.zotero.android.api.network

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CustomNetworkError(

    @SerializedName("msg")
    @Expose
    val msg: String,

    @SerializedName("code")
    @Expose
    val code: Int
) {
    fun isUnchanged(): Boolean {
        return code == 304
    }
}