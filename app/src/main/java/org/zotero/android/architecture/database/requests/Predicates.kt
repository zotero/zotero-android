package org.zotero.android.architecture.database.requests

import io.realm.RealmQuery
import org.zotero.android.architecture.database.objects.ObjectSyncState
import org.zotero.android.sync.LibraryIdentifier

fun <T> RealmQuery<T>.deleted(deleted: Boolean): RealmQuery<T> {
    return equalTo("deleted", deleted)
}

fun <T> RealmQuery<T>.deleted(deleted: Boolean, libraryId: LibraryIdentifier): RealmQuery<T> {
    return deleted(deleted = deleted).and().library(libraryId)
}

fun <T> RealmQuery<T>.library(identifier: LibraryIdentifier): RealmQuery<T> {
    return when (identifier) {
        is LibraryIdentifier.custom -> {
            equalTo("customLibraryKey", identifier.type.name)
        }
        is LibraryIdentifier.group -> {
            equalTo("groupKey", identifier.groupId)
        }
    }
}

fun <T> RealmQuery<T>.changed(): RealmQuery<T> {
    return greaterThan("rawChangedFields", 0)
}

fun <T> RealmQuery<T>.changesWithoutDeletions(libraryId: LibraryIdentifier): RealmQuery<T> {
    return changed().and().library(libraryId).and().deleted(false)
}

fun <T> RealmQuery<T>.itemChangesWithoutDeletions(libraryId: LibraryIdentifier): RealmQuery<T> {
    val changePredicate = changed().or().attachmentChanged()
    return changePredicate.and().library(libraryId).and().syncState(ObjectSyncState.synced).and()
        .deleted(false)
}

fun <T> RealmQuery<T>.attachmentChanged(): RealmQuery<T> {
    return equalTo("attachmentNeedsSync", true)
}

fun <T> RealmQuery<T>.syncState(syncState: ObjectSyncState): RealmQuery<T> {
    return equalTo("syncState", syncState.name)
}

fun <T> RealmQuery<T>.key(key: String): RealmQuery<T> {
    return equalTo("key", key)
}

fun <T> RealmQuery<T>.key(key: String, libraryId: LibraryIdentifier): RealmQuery<T> {
    return key(key).and().library(libraryId)
}

fun <T> RealmQuery<T>.key(keys: List<String>): RealmQuery<T> {
    return `in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.key(keys: Set<String>): RealmQuery<T>  {
    return `in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.parentKey(parentKey: String): RealmQuery<T> {
    return equalTo("parentKey", parentKey)
}

fun <T> RealmQuery<T>.parentKey(parentKey: String, library: LibraryIdentifier): RealmQuery<T> {
    return library(library).and().parentKey(parentKey)
}

fun <T> RealmQuery<T>.baseTagsToDelete(): RealmQuery<T> {
    return equalTo("tag.tags.@count", 1L).and().equalTo("tag.color", "")
}

fun <T> RealmQuery<T>.name(name: String): RealmQuery<T> {
    return equalTo("name", name)
}

fun <T> RealmQuery<T>.name(names: List<String>): RealmQuery<T> {
    return `in`("name", names.toTypedArray())
}

fun <T> RealmQuery<T>.keys(keys: List<String>, libraryId: LibraryIdentifier): RealmQuery<T> {
    return key(keys = keys).and().library(libraryId)
}

fun <T> RealmQuery<T>.keys(keys: Set<String>, libraryId: LibraryIdentifier): RealmQuery<T> {
    return key(keys = keys).and().library(libraryId)
}

fun <T> RealmQuery<T>.isTrash(trash: Boolean): RealmQuery<T>{
    return equalTo("trash", trash)
}