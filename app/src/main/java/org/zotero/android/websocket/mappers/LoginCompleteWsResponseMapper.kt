package org.zotero.android.websocket.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.websocket.responses.LoginCompleteWsResponse
import javax.inject.Inject

class LoginCompleteWsResponseMapper @Inject constructor(private val gson: Gson) {

    fun fromString(textToParse:String): LoginCompleteWsResponse {
        val response = gson.fromJson(textToParse, JsonObject::class.java)
        val topic = response["topic"].asString
        val userId = response["userID"].asLong
        val username = response["username"].asString
        val apiKey = response["apiKey"].asString

        return LoginCompleteWsResponse(
            topic = topic,
            userId = userId,
            username = username,
            apiKey = apiKey

        )
    }

}