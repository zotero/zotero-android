package org.zotero.android.api.mappers

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.websocket.ChangeWsResponse
import javax.inject.Inject

class ChangeWsResponseMapper @Inject constructor(private val gson: Gson) {

    fun fromString(textToParse:String): ChangeWsResponse {
        val json = gson.fromJson(textToParse, JsonObject::class.java)
        val topic = json["topic"].asString
        if (topic.contains("translators") || topic.contains("styles")) {
            return ChangeWsResponse(type = ChangeWsResponse.Kind.translators)
        }
        val libraryId = LibraryIdentifier.from(topic)
        if (libraryId != null) {
            val version = json["version"]?.asString?.toIntOrNull()
            return ChangeWsResponse(type = ChangeWsResponse.Kind.library(libraryId, version))
        }
        throw ChangeWsResponse.Error.unknownChange(topic)

    }
}