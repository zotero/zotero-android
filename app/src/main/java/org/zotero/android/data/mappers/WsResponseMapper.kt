package org.zotero.android.data.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.websocket.WsResponse
import timber.log.Timber
import javax.inject.Inject

class WsResponseMapper @Inject constructor() {

    fun fromString(textToParse:String): WsResponse {
        val json = Gson().fromJson(textToParse, JsonObject::class.java)
        val eventStr = json["event"].asString
        try {
            return WsResponse(WsResponse.Event.from(eventStr)!!)
        } catch (e: Exception) {
            Timber.e(e)
            throw WsResponse.Error.unknownEvent(eventStr)
        }
    }
}