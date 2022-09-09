package org.zotero.android.sync

import org.zotero.android.architecture.database.objects.RCustomLibraryType

sealed class LibraryIdentifier {
    data class custom(val type: RCustomLibraryType) : LibraryIdentifier()
    data class group(val groupId: Int) : LibraryIdentifier()

    val isGroupLibrary: Boolean
        get() {
            return when (this) {
                is custom -> false
                is group -> true
            }
        }

    fun apiPath(userId: Int): String {
        return when (val q = this) {
            is group ->
                "groups/${q.identifier}"
            is custom ->
                "users/${userId}"
        }
    }

    companion object {
        fun from(apiPath: String): LibraryIdentifier? {
            if (apiPath.contains("users")) {
                return LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
            }

            val lastSeparator = apiPath.indexOfLast { it == '/' }
            if (apiPath.contains("groups") && lastSeparator != -1) {
                val groupId = apiPath.substring(startIndex = lastSeparator + 1).toIntOrNull()
                if (groupId != null) {
                    LibraryIdentifier.group(groupId)
                }
            }

            return null
        }
    }
}
