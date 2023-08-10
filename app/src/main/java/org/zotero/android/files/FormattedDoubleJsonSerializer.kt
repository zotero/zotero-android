package org.zotero.android.files

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.DecimalFormat

class FormattedDoubleJsonSerializer : JsonSerializer<Double> {

    override fun serialize(
        src: Double?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val df = DecimalFormat("#.###")
        return JsonPrimitive(df.format(src).toDouble())
    }
}