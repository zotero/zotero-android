package org.zotero.android.api.pojo.sync

import org.zotero.android.architecture.database.objects.RCustomLibraryType
import org.zotero.android.sync.LibraryIdentifier

data class LibraryResponse(
    val id: Int,
    val name: String,
    val type: String,
    val links: LinksResponse?
) {

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