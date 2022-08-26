package org.zotero.android.api.pojo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class LoginRequestAccessPart(

    @SerializedName("user")
    @Expose
    val user: LoginRequestUserPart = LoginRequestUserPart(),

    @SerializedName("groups")
    @Expose
    val groups: LoginRequestGroupsPart = LoginRequestGroupsPart(),
)
