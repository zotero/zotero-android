package org.zotero.android.sync

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.zotero.android.database.objects.RCustomLibraryType

sealed class LibraryIdentifier: Parcelable {
    @Parcelize
    data class custom(val type: RCustomLibraryType) : LibraryIdentifier()
    @Parcelize
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
        when (this) {
            is LibraryIdentifier.custom -> {
                when (this.type) {
                    RCustomLibraryType.myLibrary ->
                    return "custom_my_library"
                }
            }

            is LibraryIdentifier.group ->
            return "group_${this.groupId}"
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
