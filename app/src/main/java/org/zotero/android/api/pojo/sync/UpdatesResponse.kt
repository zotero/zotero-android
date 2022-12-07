package org.zotero.android.api.pojo.sync

import com.google.gson.JsonObject

data class UpdatesResponse(
    val successful:Map<String, String>,
    val successfulJsonObjects:Map<String, JsonObject>,
    val unchanged:Map<String, String>,
    val failed:List<FailedUpdateResponse>,
)

data class FailedUpdateResponse(
    val key :String?,
    val code :Int,
    val message :String,
)
