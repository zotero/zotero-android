package org.zotero.android.files

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.zotero.android.ktx.rounded
import java.lang.reflect.Type

class FormattedDoubleJsonSerializer : JsonSerializer<Double> {

    override fun serialize(
        src: Double?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null) {
            return JsonNull.INSTANCE
        }
        val roundedResult = src.rounded(3)
        return JsonPrimitive(roundedResult)
    }
}