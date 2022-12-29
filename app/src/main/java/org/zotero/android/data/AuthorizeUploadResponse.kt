package org.zotero.android.data

import com.google.gson.JsonObject

sealed class AuthorizeUploadResponse {
    data class exists(val version: Int): AuthorizeUploadResponse()
    data class new(val authorizeNewUploadResponse: AuthorizeNewUploadResponse): AuthorizeUploadResponse()

    companion object {

        fun fromJson(data: JsonObject, lastModifiedVersion: Int): AuthorizeUploadResponse {
            if (data["exists"] != null) {
                return AuthorizeUploadResponse.exists(lastModifiedVersion)
            } else {
                return AuthorizeUploadResponse.new(AuthorizeNewUploadResponse.fromJson(data))
            }

        }
    }
}