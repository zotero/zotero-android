package org.zotero.android.screens.itemdetails.data

sealed class ItemDetailError: Exception() {
    sealed class AttachmentAddError : Exception() {
        data class couldNotMoveFromSource(val names: List<String>) : AttachmentAddError()
        data class someFailedCreation(val names: List<String>) : AttachmentAddError()
        object allFailedCreation : AttachmentAddError()
    }

    data class typeNotSupported(val type: String) : ItemDetailError()
    object cantStoreChanges : ItemDetailError()
    data class droppedFields(val fields: List<String>) : ItemDetailError()
    object cantCreateData : ItemDetailError()
    object cantTrashItem : ItemDetailError()
    object cantSaveNote : ItemDetailError()
    data class cantAddAttachments(val attachmentError: AttachmentAddError) : ItemDetailError()
    object cantSaveTags : ItemDetailError()
    object cantRemoveItem : ItemDetailError()
    object cantRemoveParent : ItemDetailError()
    object itemWasChangedRemotely : ItemDetailError()
    object askUserToDeleteOrRestoreItem : ItemDetailError()
}