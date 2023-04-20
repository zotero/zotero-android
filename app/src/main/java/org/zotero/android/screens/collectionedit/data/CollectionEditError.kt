package org.zotero.android.screens.collectionedit.data

sealed class CollectionEditError {
    object saveFailed : CollectionEditError()
    object askUserToDeleteOrRestoreCollection : CollectionEditError()
}
