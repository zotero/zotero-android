package org.zotero.android.screens.share.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

sealed interface CollectionPickerState {
    object loading : CollectionPickerState
    object failed : CollectionPickerState
    data class picked(val library: Library, val collection: Collection?) : CollectionPickerState
}