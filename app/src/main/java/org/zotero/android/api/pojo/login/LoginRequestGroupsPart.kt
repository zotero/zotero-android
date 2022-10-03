package org.zotero.android.api.pojo.login

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginRequestGroupsPart(

    @SerializedName("all")
    @Expose
    val all: Map<String, Boolean> = mapOf("library" to true, "write" to true),
    )
