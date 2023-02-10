package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.UserResponse
import javax.inject.Inject

class UserResponseMapper @Inject constructor() {

    fun fromJson(json: JsonObject): UserResponse {
        val id = json["id"].asInt
        val name = json["name"].asString
        val username = json["username"].asString
        return UserResponse(id = id, name = name, username = username)
    }
}