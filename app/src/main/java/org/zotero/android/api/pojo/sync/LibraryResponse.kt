package org.zotero.android.api.pojo.sync

import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.sync.LibraryIdentifier

data class LibraryResponse(
    val id: Int,
    val name: String,
    val type: String,
    val links: LinksResponse?
) {

    companion object {
        fun init(libraryId: LibraryIdentifier): LibraryResponse {
            val id: Int
            val type: String
            when (libraryId) {
                is LibraryIdentifier.custom -> {
                    id = 0
                    type = "user"
                }
                is LibraryIdentifier.group -> {
                    id = libraryId.groupId
                    type = "group"
                }
            }
            return LibraryResponse(
                name = "",
                links = null,
                id = id,
                type = type,
            )
        }
    }

    val libraryId: LibraryIdentifier?
        get() {
            return when (this.type) {
                "user" ->
                    LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
                "group" ->
                    LibraryIdentifier.group(this.id)
                else -> null
            }
        }
}