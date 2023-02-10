package org.zotero.android.api.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.SearchResponse
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.ktx.convertFromBooleanOrIntToBoolean
import org.zotero.android.sync.SchemaError
import javax.inject.Inject

class SearchResponseDataMapper @Inject constructor(
    private val conditionResponseMapper: ConditionResponseMapper
) {
    fun fromJson(response: JsonObject, key: String): SearchResponse.Data {
        val unknownKey =
            response.keySet().firstOrNull { !FieldKeys.Search.knownDataKeys.contains(it) }
        if (unknownKey != null) {

            throw SchemaError.unknownField(key = key, field = unknownKey)
        }

        val name = response["name"].asString
        val conditions =
            response["conditions"].asJsonArray.map { conditionResponseMapper.fromJson(it.asJsonObject) }
        val isTrash = response["deleted"].convertFromBooleanOrIntToBoolean()

        return SearchResponse.Data(
            name = name,
            conditions = conditions,
            isTrash = isTrash
        )
    }

}
