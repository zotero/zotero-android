package org.zotero.android.screens.allitems.data

sealed class ItemsError  {
    sealed class AttachmentLoading {
        object couldNotSave : AttachmentLoading()
        data class someFailed(val k: List<String>) : AttachmentLoading()
    }

    object dataLoading: ItemsError()
    object deletion: ItemsError()
    object deletionFromCollection: ItemsError()
    object collectionAssignment: ItemsError()
    object itemMove: ItemsError()
    object noteSaving: ItemsError()
    data class attachmentAdding(val q: AttachmentLoading): ItemsError()
    object duplicationLoading: ItemsError()
}