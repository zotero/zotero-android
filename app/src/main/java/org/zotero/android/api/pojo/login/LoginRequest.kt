package org.zotero.android.api.pojo.login

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginRequest(

    @SerializedName("username")
    @Expose
    val username: String,

    @SerializedName("password")
    @Expose
    val password: String,

    @SerializedName("name")
    @Expose
    val name: String = "Automatic Zotero Android Client Key",

    @SerializedName("access")
    @Expose
    val access: LoginRequestAccessPart = LoginRequestAccessPart()


)
