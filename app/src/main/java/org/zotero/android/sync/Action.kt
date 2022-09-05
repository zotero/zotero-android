package org.zotero.android.sync;

sealed class Action {
    object loadKeyPermissions : Action()
    object syncGroupVersions : Action()
    data class createLibraryActions(
        val librarySyncType: LibrarySyncType,
        val createLibraryActionsOptions: CreateLibraryActionsOptions
    ) : Action()

    val libraryId: LibraryIdentifier?
        get() {
            when (this) {
                is loadKeyPermissions, is createLibraryActions, is syncGroupVersions ->
                    return null
            }
        }

    val requiresConflictReceiver: Boolean get() {
        when (this) {
            is loadKeyPermissions, is createLibraryActions,is syncGroupVersions ->
            return false
        }
    }

}
