package org.zotero.android.data

import com.google.gson.JsonObject
import org.zotero.android.ktx.unmarshalLinkedHashMap
import org.zotero.android.sync.Parsing

data class AuthorizeNewUploadResponse(
    val url: String,
    val uploadKey: String,
    val params: LinkedHashMap<String, String>,
) {
    companion object {

        fun fromJson(data: JsonObject): AuthorizeNewUploadResponse {
            val url = data["url"]?.asString?.replace("\\", "")
            if (url == null) {
                throw Parsing.Error.missingKey("url")
            }

            val uploadKey = data["uploadKey"].asString
            val params = data["params"].unmarshalLinkedHashMap<String, String>()!!
            return AuthorizeNewUploadResponse(
                url = url,
                uploadKey = uploadKey,
                params = params,
            )

        }
    }
}