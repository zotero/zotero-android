package org.zotero.android.api.pojo.sync

import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import timber.log.Timber

data class PageIndexResponse(
    val key: String,
    val value: String,
    val version: Int,
    val libraryId: LibraryIdentifier,
) {

    companion object {

        fun parse(key: String): Pair<String, LibraryIdentifier> {
            val parts = key.split("_")
            if (parts.size != 3) {
                Timber.e("PageIndexResponse: key is invalid format - $key")
                throw Parsing.Error.incompatibleValue(key)
            }

            val libraryPart = parts[1]
            val libraryId: LibraryIdentifier

            when (libraryPart[0]) {
                'u' -> {
                    libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
                }
                'g' -> {
                    val groupId = libraryPart.substring(startIndex = 1).toIntOrNull()
                        ?: throw Parsing.Error.incompatibleValue("groupId=$libraryPart")
                    libraryId = LibraryIdentifier.group(groupId)
                }
                else -> {
                    Timber.e("PageIndexResponse: key is invalid format - $key")
                    throw Parsing.Error.incompatibleValue("libraryPart=$libraryPart")

                }
            }
            return Pair(parts[2], libraryId)
        }
    }
}
