package org.zotero.android.sync;

sealed class Action {
    object loadKeyPermissions : Action()
    object syncGroupVersions : Action()
    data class createLibraryActions(
        val librarySyncType: LibrarySyncType,
        val createLibraryActionsOptions: CreateLibraryActionsOptions
    ) : Action()

    data class resolveDeletedGroup(val a: Int, val b: String) : Action()

    data class syncGroupToDb(val a: Int) : Action()

    val libraryId: LibraryIdentifier?
        get() {
            when (this) {
                is loadKeyPermissions, is createLibraryActions, is syncGroupVersions ->
                    return null
                is resolveDeletedGroup -> return LibraryIdentifier.group(this.a)
                is syncGroupToDb -> return LibraryIdentifier.group(this.a)
            }
        }

    val requiresConflictReceiver: Boolean
        get() {
            return when (this) {
                is loadKeyPermissions, is createLibraryActions, is syncGroupVersions ->
                    return false
                is resolveDeletedGroup -> true
                is syncGroupToDb -> false
            }
        }

}
