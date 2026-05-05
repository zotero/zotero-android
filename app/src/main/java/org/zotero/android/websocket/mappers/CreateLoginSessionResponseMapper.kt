package org.zotero.android.websocket.mappers

import com.google.gson.JsonObject
import org.zotero.android.websocket.responses.CreateLoginSessionResponse
import javax.inject.Inject

class CreateLoginSessionResponseMapper @Inject constructor() {

    fun fromJson(response: JsonObject): CreateLoginSessionResponse {
        val sessionToken = response["sessionToken"].asString
        val loginUrl = response["loginURL"].asString

        return CreateLoginSessionResponse(sessionToken =sessionToken, loginUrl = loginUrl)
    }

}