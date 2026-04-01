package org.zotero.android.websocket.mappers

import com.google.gson.JsonObject
import org.zotero.android.websocket.responses.CheckLoginSessionResponse
import org.zotero.android.websocket.responses.CheckLoginSessionResponse.Status
import javax.inject.Inject

class CheckLoginSessionResponseMapper @Inject constructor() {

    fun fromJson(response: JsonObject): CheckLoginSessionResponse {
        val status = when (val rawStatus = response["status"].asString) {
            "pending" -> {
                Status.pending
            }

            "completed" -> {
                val apiKey = response["apiKey"].asString
                val userId = response["userID"].asLong
                val username = response["username"].asString
                Status.completed(apiKey = apiKey, userId = userId, username = username)
            }

            "cancelled" -> {
                Status.cancelled
            }

            else -> {
                throw RuntimeException("Invalid status = $rawStatus")
            }
        }
        return CheckLoginSessionResponse(status = status)
    }

}