package org.zotero.android.api.pojo.login

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginRequestUserPart(

    @SerializedName("library")
    @Expose
    val library: Boolean = true,

    @SerializedName("notes")
    @Expose
    val notes: Boolean = true,

    @SerializedName("write")
    @Expose
    val write: Boolean = true,

    @SerializedName("files")
    @Expose
    val files: Boolean = true,
    )
