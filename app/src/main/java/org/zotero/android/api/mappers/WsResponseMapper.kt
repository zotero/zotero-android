package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.websocket.WsResponse
import timber.log.Timber
import javax.inject.Inject

class WsResponseMapper @Inject constructor(private val gson: Gson) {

    fun fromString(textToParse:String): WsResponse {
        val json = gson.fromJson(textToParse, JsonObject::class.java)
        val eventStr = json["event"].asString
        try {
            return WsResponse(WsResponse.Event.from(eventStr)!!)
        } catch (e: Exception) {
            Timber.e(e)
            throw WsResponse.Error.unknownEvent(eventStr)
        }
    }
}