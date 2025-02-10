package org.zotero.android.screens.retrievemetadata.data

sealed interface RetrieveMetadataState {
    object loading : RetrieveMetadataState
    object recognizedDataIsEmpty : RetrieveMetadataState
    data class failed(val message: String) : RetrieveMetadataState
    object success : RetrieveMetadataState
    object fileIsNotPdf : RetrieveMetadataState
}