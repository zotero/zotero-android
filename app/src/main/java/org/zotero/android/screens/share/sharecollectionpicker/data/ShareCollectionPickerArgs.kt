package org.zotero.android.screens.share.sharecollectionpicker.data

import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

data class ShareCollectionPickerArgs(
    val selectedCollectionId: CollectionIdentifier,
    val selectedLibraryId: LibraryIdentifier,
)

