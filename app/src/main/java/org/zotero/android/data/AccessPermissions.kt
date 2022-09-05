package org.zotero.android.data

import com.google.gson.JsonObject

data class AccessPermissions(
    val user: Permissions,
    val groupDefault: Permissions?,
    val groups: Map<Int, Permissions>,
) {
    data class Permissions(
        val library: Boolean,
        val notes: Boolean,
        val files: Boolean,
        val write: Boolean,
    ) {
        companion object {
            fun fromJson(data: JsonObject?): Permissions {
                val library = data?.get("library")?.asBoolean ?: false
                val write = data?.get("write")?.asBoolean ?: false
                val notes = data?.get("notes")?.asBoolean ?: write
                val files = data?.get("files")?.asBoolean ?: write
                return Permissions(library = library, notes = notes, files = files, write = write)
            }
        }

    }


}

