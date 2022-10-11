package org.zotero.android.files

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Marshals and unmarshals objects of any complexity to and from Json string.
 */
@Singleton
class DataMarshaller @Inject constructor(val gson: Gson) {

    fun <T> marshal(objectToMarshal: T): String {
        return gson.toJson(objectToMarshal)
    }

    inline fun <reified T> unmarshal(data: String): T {
        return gson.fromJson(data, T::class.java)
    }

    inline fun <reified K> unmarshalList(jsonString: String): MutableList<K>? {
        return try {
            val type = TypeToken.getParameterized(MutableList::class.java, K::class.java).type
            return gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Timber.e("Error parsing json list = $this")
            null
        }
    }

    inline fun <reified K, reified V> unmarshalMap(jsonString: String): MutableMap<K, V>? {
        return try {
            val type = TypeToken.getParameterized(MutableMap::class.java, K::class.java, V::class.java).type
            return gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            Timber.e("Error parsing json map = $this")
            null
        }
    }
}