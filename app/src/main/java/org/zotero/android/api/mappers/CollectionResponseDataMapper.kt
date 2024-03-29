package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.CollectionResponse
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.ktx.convertFromBooleanOrIntToBoolean
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
        val parentCollectionAsString = response["parentCollection"]?.asString
        val parentCollection = if( parentCollectionAsString == "false") {
            null
        } else {
            parentCollectionAsString
        }

        val isTrash = response["deleted"].convertFromBooleanOrIntToBoolean()

        return CollectionResponse.Data(
            name = name,
            parentCollection = parentCollection,
            isTrash = isTrash
        )
    }

}
