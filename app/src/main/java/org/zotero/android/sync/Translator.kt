package org.zotero.android.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import timber.log.Timber

data class Translator(
    val metadata: MutableMap<String, Any>,
    val code: String,
) {
    companion object {
        fun fromMetadata(
            attributeCount: Int,
            getAttributeName: (Int) -> String,
            getAttributeValue: (Int) -> String,
            code: String,
            gson: Gson
        ): Translator {
            val updatedMetadata = mutableMapOf<String, Any>()
            for (i in 0..<attributeCount) {
                val key = getAttributeName(i)
                val value = getAttributeValue(i)
                when (key) {
                    "id" -> {
                        updatedMetadata["translatorID"] = value(
                            string = value,
                            gson = gson
                        )

                    }

                    "type" -> {
                        updatedMetadata["translatorType"] = value(
                            string = value,
                            gson = gson
                        )
                    }

                    else -> {
                        updatedMetadata[key] = value(string = value, gson = gson)
                    }
                }
            }
            return Translator(metadata = updatedMetadata, code = code)
        }


        private fun value(string: String, gson: Gson): Any {
            val intValue = string.toIntOrNull()
            if (intValue != null) {
                return intValue
            }

            val lowercased = string.lowercase()
            if (lowercased == "true") {
                return true
            }
            if (lowercased == "false") {
                return false
            }

            if (string.first() != '{') {
                return string
            }
            try {
                return gson.fromJson(string, JsonObject::class.java)
            } catch (e: Exception) {
                Timber.e(e)
            }

            return string
        }
    }

    fun withMetadata(key: String, value: String, gson: Gson): Translator {
        val metadata = this.metadata
        when (key) {
            "id" -> {
                metadata["translatorID"] = value(string = value, gson = gson)
            }

            "type" -> {
                metadata["translatorType"] = value(string = value, gson = gson)
            }

            else -> {
                metadata[key] = value(string = value, gson = gson)
            }
        }
        return Translator(metadata = metadata, code = this.code)
    }

    fun withCode(code: String): Translator {
        return Translator(metadata = this.metadata, code = code)
    }
}
