package org.zotero.android.sync.syncactions.data

import com.google.gson.Gson
import com.google.gson.JsonObject

sealed class AuthorizeUploadResponse {
    data class exists(val version: Int): AuthorizeUploadResponse()
    data class new(val authorizeNewUploadResponse: AuthorizeNewUploadResponse): AuthorizeUploadResponse()

    companion object {

        fun fromJson(
            data: JsonObject,
            lastModifiedVersion: Int,
            gson: Gson
        ): AuthorizeUploadResponse {
            if (data["exists"] != null) {
                return exists(lastModifiedVersion)
            } else {
                return new(AuthorizeNewUploadResponse.fromJson(data = data, gson = gson))
            }

        }
    }
}