package org.zotero.android.sync.syncactions.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.zotero.android.ktx.unmarshalLinkedHashMap
import org.zotero.android.sync.Parsing
import timber.log.Timber

data class AuthorizeNewUploadResponse(
    val url: String,
    val uploadKey: String,
    val params: LinkedHashMap<String, String>,
) {
    companion object {

        fun fromJson(data: JsonObject, gson: Gson): AuthorizeNewUploadResponse {
            val url = data["url"]?.asString?.replace("\\", "")
            if (url == null) {
                Timber.e("AuthorizeNewUploadResponse: url invalid format - $url")
                throw Parsing.Error.missingKey("url")
            }

            val uploadKey = data["uploadKey"].asString
            val params = data["params"].unmarshalLinkedHashMap<String, String>(gson)!!
            return AuthorizeNewUploadResponse(
                url = url,
                uploadKey = uploadKey,
                params = params,
            )

        }
    }
}