package org.zotero.android.sync.syncactions.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.ktx.unmarshalMap

data class KeyResponse(
    val username: String = "",
    val displayName: String = "",
    val user: AccessPermissions.Permissions = AccessPermissions.Permissions.fromJson(null),
    val defaultGroup: AccessPermissions.Permissions? = AccessPermissions.Permissions.fromJson(null),
    val groups: Map<Int, AccessPermissions.Permissions> = mapOf()
) {

    companion object {
        fun fromJson(data: JsonObject, gson: Gson): KeyResponse {
            val accessData = data["access"].asJsonObject

            val username = data.get("username")?.asString ?: ""
            val displayName = data.get("displayName")?.asString ?: ""

            val libraryData = accessData["user"]?.asJsonObject
            val user = AccessPermissions.Permissions.fromJson(libraryData)

            val groupDataMap: Map<String, JsonObject>? = accessData["groups"]?.unmarshalMap(gson)

            var defaultGroup: AccessPermissions.Permissions? = null
            val groups = mutableMapOf<Int, AccessPermissions.Permissions>()

            groupDataMap?.let {
                for (groupData in groupDataMap) {
                    if (groupData.key == "all") {
                        defaultGroup = AccessPermissions.Permissions.fromJson(groupData.value)
                    } else {
                        val intKey = groupData.key.toIntOrNull()
                        if (intKey != null) {
                            groups[intKey] = AccessPermissions.Permissions.fromJson(groupData.value)
                        }
                    }
                }
            }
            return KeyResponse(
                username = username,
                user = user,
                displayName = displayName,
                defaultGroup = defaultGroup,
                groups = groups
            )
        }
    }


}