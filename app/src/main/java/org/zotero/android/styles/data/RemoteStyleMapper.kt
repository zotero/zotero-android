package org.zotero.android.styles.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.zotero.android.helpers.formatter.sqlFormat
import org.zotero.android.sync.Parsing
import timber.log.Timber
import java.net.URL
import java.util.Date
import javax.inject.Inject

class RemoteStyleMapper @Inject constructor() {

    fun fromJson(jsonArray: JsonArray): List<RemoteStyle> {
        return jsonArray.mapNotNull { j ->
            try {
                val style = fromJson(j.asJsonObject)
                style
            } catch (e: Exception) {
                Timber.w(e, "CitationStylesResponse: can't parse style")
                null
            }
        }

    }

    private fun fromJson(json: JsonObject): RemoteStyle {
        val rawDate = json["updated"].asString
        val rawHref = json["href"].asString

        val href: String
        try {
            URL(rawHref)
            href = rawHref
        } catch (e: Exception) {
            throw Parsing.Error.notUrl
        }
        val title = json["title"].asString
        val name = json["name"].asString

        val dependent = json["dependent"].asInt == 1

        val category = parseRemoteCitationStyleCategory(json["categories"].asJsonObject)
        var updated: Date
        try {
            updated = sqlFormat.parse(rawDate)!!
        } catch (e: Exception) {
            Timber.e(e)
            updated = Date(0L)
        }
        return RemoteStyle(
            title = title,
            name = name,
            dependent = dependent,
            category = category,
            updated = updated,
            href = href,
        )

    }

    private fun parseRemoteCitationStyleCategory(json: JsonObject): RemoteCitationStyleCategory {
        val format = json["format"].asString
        val fields = json["fields"].asJsonArray.map { it.asString }
        return RemoteCitationStyleCategory(
            format = format,
            fields = fields,
        )
    }

}