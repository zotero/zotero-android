package org.zotero.android.api.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginResponse(

    @SerializedName("userID")
    @Expose
    val userId: Long,

    @SerializedName("name")
    @Expose
    val name: String,

    @SerializedName("displayName")
    @Expose
    val displayName: String,

    @SerializedName("key")
    @Expose
    val key: String,
)
