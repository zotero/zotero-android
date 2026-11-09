package org.zotero.android.speech.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

sealed class ReadAloudResumePosition {
    data class Pdf(val pageIndex: Int, val rects: List<SDTRect>) : ReadAloudResumePosition()
    data class Reader(val source: JsonObject) : ReadAloudResumePosition()

    val json: JsonObject
        get() = when (this) {
            is Pdf -> JsonObject().apply {
                addProperty("pageIndex", pageIndex)
                add("rects", JsonArray().apply {
                    rects.forEach { rect ->
                        add(JsonArray().apply {
                            add(rect.x)
                            add(rect.y)
                            add(rect.x + rect.width)
                            add(rect.y + rect.height)
                        })
                    }
                })
            }

            is Reader -> source
        }

    companion object {
        fun from(json: JsonObject): ReadAloudResumePosition? {
            val pageIndex = json["pageIndex"]?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isNumber }?.asInt
            if (pageIndex != null) {
                val rects = json["rects"]?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.mapNotNull { element -> element.takeIf { it.isJsonArray }?.let { rect(it.asJsonArray) } }
                    .orEmpty()
                return Pdf(pageIndex, rects)
            }
            val type = json["type"]
            if (type != null && type.isJsonPrimitive && type.asJsonPrimitive.isString) {
                return Reader(json)
            }
            return null
        }

        private fun rect(values: JsonArray): SDTRect? {
            if (values.size() != 4) {
                return null
            }
            val doubles = values.mapNotNull { number(it) }
            if (doubles.size != 4) {
                return null
            }
            return SDTRect(
                x = doubles[0],
                y = doubles[1],
                width = doubles[2] - doubles[0],
                height = doubles[3] - doubles[1],
            )
        }

        private fun number(element: JsonElement): Double? {
            val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
            return if (primitive.isNumber) primitive.asDouble else primitive.asString.toDoubleOrNull()
        }
    }
}