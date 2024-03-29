package org.zotero.android.ktx

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import timber.log.Timber

/**
    Extension functions for streamlining the most common json marshalling operations.
 */

/**
 * Unmarshals JsonElement into a single object of a given class
 * @return Unmarshalled object or null if JsonElement was null or parsing failed
 */
inline fun <reified K> JsonElement?.unmarshal(gson: Gson): K? {
    return try {
        return gson.fromJson(this, K::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Error parsing json object = $this")
        null
    }
}

/**
 * Unmarshals JsonElement into a map of specified key-value pairs
 * @return Unmarshalled map or null if JsonElement was null or parsing failed
 */
inline fun <reified K, reified V> JsonElement?.unmarshalMap(gson: Gson): Map<K, V>? {
    if (this == null || isJsonNull) {
        return null
    }
    return try {
        val type = TypeToken.getParameterized(MutableMap::class.java, K::class.java, V::class.java).type
        return gson.fromJson(this, type)
    } catch (e: Exception) {
        Timber.e(e, "Error parsing json map = $this")
        null
    }
}

inline fun <reified K, reified V> JsonElement?.unmarshalLinkedHashMap(gson: Gson): LinkedHashMap<K, V>? {
    if (this == null || isJsonNull) {
        return null
    }
    return try {
        val type = TypeToken.getParameterized(LinkedHashMap::class.java, K::class.java, V::class.java).type
        return gson.fromJson(this, type)
    } catch (e: Exception) {
        Timber.e(e, "Error parsing json map = $this")
        null
    }
}

fun JsonElement?.unmarshalMapOfListToInt(gson: Gson): Map<String, List<Int>>? {
    return try {
        val type = object : TypeToken<Map<String, List<Int>>>() {}.type
        return gson.fromJson(this, type)
    } catch (e: Exception) {
        Timber.e(e, "Error parsing json map of list of ints = $this")

        null
    }
}

/**
 * Unmarshals JsonElement into a list of objects of specified class
 * @return Unmarshalled list or null if JsonElement was null or parsing failed
 */
inline fun <reified K> JsonElement?.unmarshalList(gson: Gson): List<K>? {
    return try {
        val type = TypeToken.getParameterized(List::class.java, K::class.java).type
        return gson.fromJson(this, type)
    } catch (e: Exception) {
        Timber.e("Error parsing json array = $this")
        null
    }
}

fun JsonElement?.convertFromBooleanOrIntToBoolean(): Boolean {
    val jsonPrimitive = this?.asJsonPrimitive
    if (jsonPrimitive == null) {
        return false
    }
    return when {
        jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
        jsonPrimitive.isNumber -> jsonPrimitive.asInt == 1
        else -> false
    }
}