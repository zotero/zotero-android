package org.zotero.android.uicomponents.addbyidentifier.data

sealed interface InitializationResult {
    object initialized : InitializationResult
    object inProgress : InitializationResult
    data class failed(val error: Exception) : InitializationResult
}
