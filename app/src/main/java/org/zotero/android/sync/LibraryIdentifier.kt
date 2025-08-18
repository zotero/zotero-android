package org.zotero.android.sync

import kotlinx.serialization.Serializable
import org.zotero.android.database.objects.RCustomLibraryType

@Serializable
sealed class LibraryIdentifier: java.io.Serializable  {

    @Serializable
    data class custom(val type: RCustomLibraryType) : LibraryIdentifier()

    @Serializable
    data class group(val groupId: Int) : LibraryIdentifier()

    val isGroupLibrary: Boolean
        get() {
            return when (this) {
                is custom -> false
                is group -> true
            }
        }

    fun apiPath(userId: Long): String {
        return when (val q = this) {
            is group ->
                "groups/${q.groupId}"
            is custom ->
                "users/${userId}"
        }
    }

    val folderName: String get() {
        return when (this) {
            is LibraryIdentifier.custom -> {
                when (this.type) {
                    RCustomLibraryType.myLibrary ->
                        "custom_my_library"
                }
            }

            is LibraryIdentifier.group ->
                "group_${this.groupId}"
        }
    }

    val debugName: String get() {
        return when (this) {
            is group -> {
                "Group ($groupId)"
            }

            is custom -> {
                when (this.type) {
                    RCustomLibraryType.myLibrary -> {
                        "My Library"
                    }
                }
            }
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
                    return LibraryIdentifier.group(groupId)
                }
            }

            return null
        }
    }
}
