package org.zotero.android.websocket.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.websocket.WsResponse
import org.zotero.android.websocket.responses.LoginWsResponse
import javax.inject.Inject

class LoginCompleteWsResponseMapper @Inject constructor(private val gson: Gson) {

    fun fromString(textToParse: String): LoginWsResponse {
        val response = gson.fromJson(textToParse, JsonObject::class.java)

        val eventString = response["event"].asString

        val event =
            WsResponse.Event.from(eventString) ?: throw WsResponse.Error.unknownEvent(eventString)

        val topic = response["topic"].asString

        val kind = when (event) {
            WsResponse.Event.loginComplete -> {
                val userId = response["userID"].asLong
                val username = response["username"].asString
                val apiKey = response["apiKey"].asString
                LoginWsResponse.Kind.complete(
                    topicK = topic,
                    userId = userId,
                    username = username,
                    apiKey = apiKey
                )
            }

            WsResponse.Event.loginCancelled -> {
                LoginWsResponse.Kind.cancelled(topic)
            }

            else -> {
                throw WsResponse.Error.unknownEvent(event.name)
            }
        }
        return LoginWsResponse(
            kind = kind,
        )
    }

}