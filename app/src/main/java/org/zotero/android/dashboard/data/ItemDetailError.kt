package org.zotero.android.dashboard.data

sealed class ItemDetailError: Exception() {
    sealed class AttachmentAddError: Exception(){
        data class couldNotMoveFromSource(val list: List<String>): AttachmentAddError()
        data class someFailedCreation(val list: List<String>): AttachmentAddError()
        object allFailedCreation: AttachmentAddError()
    }

    data class typeNotSupported(val str: String): ItemDetailError()
    object cantStoreChanges: ItemDetailError()
    data class droppedFields(val list: List<String>): ItemDetailError()
    object cantCreateData: ItemDetailError()
    object cantTrashItem: ItemDetailError()
    object cantSaveNote: ItemDetailError()
    data class cantAddAttachments(val attachmentError: AttachmentAddError): ItemDetailError()
    object cantSaveTags: ItemDetailError()
    object cantRemoveDuplicatedItem: ItemDetailError()
}