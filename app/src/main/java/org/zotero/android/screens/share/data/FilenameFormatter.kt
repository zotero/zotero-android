package org.zotero.android.screens.share.data

import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.sync.DateParser

object FilenameFormatter {

    fun filename(
        item: ItemResponse,
        defaultTitle: String,
        ext: String,
        dateParser: DateParser
    ): String {
        var filename = ""
        val creators = creators(item)
        if (creators != null) {
            filename = creators
        }
        val year = year(item, dateParser = dateParser)
        if (year != null) {
            filename += " - " + year
        }

        val title =
            item.fields[KeyBaseKeyPair(key = FieldKeys.Item.Attachment.title, baseKey = null)]
                ?: defaultTitle

        if (filename.isEmpty()) {
            return title + "." + ext
        }

        return validate(filename = filename + " - " + title + "." + ext)
    }

    fun validate(filename: String): String {
        var valid = filename.replace("[/\\\\?*:|\"<>]".toRegex(), "")
        valid = valid.replace("[\\r\\n\\t]".toRegex(), " ")
        valid = valid.replace("[\\u2000-\\u200A]".toRegex(), " ")
        valid = valid.replace("[\\u200B-\\u200E]".toRegex(), "")

        if (valid.isEmpty() || valid == "." || valid == "..") {
            return "_"
        }
        if (valid[0] == '.') {
            return valid.substring(1)
        }
        return valid
    }

    private fun creators(item: ItemResponse): String? {
        val creators = item.creators
        when (creators.size) {
            0 -> {
                return null
            }

            1 -> {
                return creators.firstOrNull()?.summaryName
            }

            2 -> {
                return "${creators.firstOrNull()?.summaryName ?: ""} and ${creators.lastOrNull()?.summaryName ?: ""}"
            }

            else -> {
                return "${creators.firstOrNull()?.summaryName ?: ""} et al."
            }
        }
    }

    private fun year(item: ItemResponse, dateParser: DateParser): String? {
        return item
            .fields[KeyBaseKeyPair(key = FieldKeys.Item.date, baseKey = null)]
            ?.let { dateParser.parse(it) }
            ?.let { "${it.year}" }
    }
}