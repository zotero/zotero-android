package org.zotero.android.sync

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RGroup

@Parcelize
data class Library(
    val identifier: LibraryIdentifier,
    val name: String,
    val metadataEditable: Boolean,
    val filesEditable: Boolean,
): Parcelable {
    val id: LibraryIdentifier get() {
        return this.identifier
    }

    constructor(customLibrary: RCustomLibrary): this(
        identifier = LibraryIdentifier.custom(RCustomLibraryType.valueOf(customLibrary.type)),
        name = RCustomLibraryType.valueOf(customLibrary.type).libraryName,
        metadataEditable = true,
        filesEditable = true,
    )

    constructor(group: RGroup) : this(
        identifier = LibraryIdentifier.group(group.identifier),
        name = group.name,
        metadataEditable = group.canEditMetadata,
        filesEditable = group.canEditFiles,
    )
}
