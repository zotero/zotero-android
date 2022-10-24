package org.zotero.android.data.mappers

import com.google.gson.JsonObject
import org.zotero.android.api.pojo.sync.ConditionResponse
import javax.inject.Inject

class ConditionResponseMapper @Inject constructor(
) {
    fun fromJson(response: JsonObject): ConditionResponse {
        val name = response["condition"].asString
        val operator = response["operator"].asString
        val value = response["value"].asString

        return ConditionResponse(
            condition = name, operator = operator, value = value
        )
    }

}
