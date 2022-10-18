package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.sync.SchemaError
import javax.inject.Inject

class CollectionResponseDataMapper @Inject constructor(
) {
    fun fromJson(response: JsonObject, key: String): CollectionResponse.Data {
        val unknownKey =
            response.keySet().firstOrNull { !FieldKeys.Collection.knownDataKeys.contains(it) }
        if (unknownKey != null) {

            throw SchemaError.unknownField(key = key, field = unknownKey)
        }

        val name = response["name"].asString
        val parentCollection = response["parentCollection"]?.asString
        val isTrash = response["deleted"]?.asBoolean ?: (response["deleted"]?.asInt == 1)

        return CollectionResponse.Data(
            name = name,
            parentCollection = parentCollection,
            isTrash = isTrash
        )
    }

}
