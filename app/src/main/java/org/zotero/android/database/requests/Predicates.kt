package org.zotero.android.database.requests

import io.realm.Case
import io.realm.RealmQuery
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.ObjectSyncState
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

fun <T> RealmQuery<T>.deleted(deleted: Boolean): RealmQuery<T> {
    return equalTo("deleted", deleted)
}

fun <T> RealmQuery<T>.deleted(deleted: Boolean, libraryId: LibraryIdentifier): RealmQuery<T> {
    return deleted(deleted = deleted)
        .and()
        .library(libraryId)
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
    return isNotEmpty("changes")
}

fun <T> RealmQuery<T>.notChanged(): RealmQuery<T> {
    return isEmpty("changes")
}

fun <T> RealmQuery<T>.changesWithoutDeletions(libraryId: LibraryIdentifier): RealmQuery<T> {
    return changed()
        .and()
        .library(libraryId)
        .and()
        .deleted(false)
}

fun <T> RealmQuery<T>.itemChangesWithoutDeletions(libraryId: LibraryIdentifier): RealmQuery<T> {
    val changePredicate = changed()
        .or()
        .attachmentChanged()
    return changePredicate
        .and()
        .library(libraryId)
        .and()
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
    return key(key)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.key(keys: List<String>): RealmQuery<T> {
    return `in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.key(keys: Set<String>): RealmQuery<T> {
    return `in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.key(key: String, andBaseKey: String): RealmQuery<T> {
    return equalTo("key", key)
        .and()
        .equalTo("baseKey", andBaseKey)
}

fun <T> RealmQuery<T>.keyNotIn(keys: Set<String>): RealmQuery<T> {
    return not().`in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.keyNotIn(keys: List<String>): RealmQuery<T> {
    return not().`in`("key", keys.toTypedArray())
}

fun <T> RealmQuery<T>.baseKey(baseKey: String): RealmQuery<T> {
    return equalTo("baseKey", baseKey)
}

fun <T> RealmQuery<T>.parentKey(parentKey: String): RealmQuery<T> {
    return equalTo("parentKey", parentKey)
}

fun <T> RealmQuery<T>.parentKey(parentKey: String, library: LibraryIdentifier): RealmQuery<T> {
    return library(library)
        .and()
        .parentKey(parentKey)
}

fun <T> RealmQuery<T>.baseTagsToDelete(): RealmQuery<T> {
    return equalTo("tag.tags.@count", 1L)
        .and()
        .equalTo("tag.color", "")
}

fun <T> RealmQuery<T>.name(name: String): RealmQuery<T> {
    return equalTo("name", name)
}

fun <T> RealmQuery<T>.name(name: String, libraryId: LibraryIdentifier): RealmQuery<T> {
    return name(name)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.nameIn(names: List<String>): RealmQuery<T> {
    return `in`("name", names.toTypedArray())
}

fun <T> RealmQuery<T>.nameIn(names: List<String>, libraryId: LibraryIdentifier): RealmQuery<T> {
    return nameIn(names)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.keys(keys: List<String>, libraryId: LibraryIdentifier): RealmQuery<T> {
    return key(keys = keys)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.keys(keys: Set<String>, libraryId: LibraryIdentifier): RealmQuery<T> {
    return key(keys = keys)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.isTrash(trash: Boolean): RealmQuery<T> {
    return equalTo("trash", trash)
}

fun <T> RealmQuery<T>.tagNameNotIn(names: List<String>): RealmQuery<T> {
    return not().`in`("tag.name", names.toTypedArray())
}

fun <T> RealmQuery<T>.tagName(name: String): RealmQuery<T> {
    return equalTo("tag.name", name)
}

fun <T> RealmQuery<T>.attachmentNeedsUpload(): RealmQuery<T> {
    return equalTo("attachmentNeedsSync", true)
}

fun <T> RealmQuery<T>.itemsNotChangedAndNeedUpload(libraryId: LibraryIdentifier): RealmQuery<T> {
    return notChanged()
        .and()
        .attachmentNeedsUpload()
        .and()
        .item(ItemTypes.attachment)
        .and()
        .library(libraryId)
}

fun <T> RealmQuery<T>.item(type: String): RealmQuery<T> {
    return equalTo("rawType", type)
}

fun <T> RealmQuery<T>.items(
    forCollectionsKeys: Set<String>,
    libraryId: LibraryIdentifier
): RealmQuery<T> {
    val predicates = baseItemPredicates(isTrash = false, libraryId = libraryId)
        .and()
        .`in`("collections.key", forCollectionsKeys.toTypedArray())
    return predicates
}


fun <T> RealmQuery<T>.baseItemPredicates(
    isTrash: Boolean,
    libraryId: LibraryIdentifier
): RealmQuery<T> {
    var resultQuery =
        library(libraryId)
            .and()
            .notSyncState(ObjectSyncState.dirty)
            .and()
            .deleted(false).and()
            .isTrash(isTrash)
    if (!isTrash) {
        resultQuery = resultQuery.and().isNull("parent")
    }
    return resultQuery

}


fun <T> RealmQuery<T>.notSyncState(syncState: ObjectSyncState): RealmQuery<T> {
    return notEqualTo("syncState", syncState.name)
}

fun <T> RealmQuery<T>.items(
    forCollectionId: CollectionIdentifier,
    libraryId: LibraryIdentifier
): RealmQuery<T> {
    var predicates = baseItemPredicates(isTrash = forCollectionId.isTrash, libraryId = libraryId)
    when (forCollectionId) {
        is CollectionIdentifier.collection -> {
            predicates = predicates
                .and()
                .equalTo("collections.key", forCollectionId.key)
        }
        is CollectionIdentifier.custom -> {
            when (forCollectionId.type) {
                CollectionIdentifier.CustomType.unfiled -> {
                    predicates = predicates
                        .and()
                        .isEmpty("collections")
                }
                CollectionIdentifier.CustomType.all,
                CollectionIdentifier.CustomType.publications,
                CollectionIdentifier.CustomType.trash -> {
                    //no-op
                }
            }
        }
        is CollectionIdentifier.search -> {
            //no-op
        }
    }
    return predicates
}

fun <T> RealmQuery<T>.items(
    type: String,
    notSyncState: ObjectSyncState,
    trash: Boolean? = null
): RealmQuery<T> {
    var predicates = item(type = type)
        .and()
        .notSyncState(notSyncState)
    if (trash != null) {
        predicates = predicates
            .and()
            .isTrash(trash)
    }
    return predicates
}

fun <T> RealmQuery<T>.changedByUser(
): RealmQuery<T> {
    return equalTo("changeType", UpdatableChangeType.user.name)
}


fun <T> RealmQuery<T>.itemUserChanges(
): RealmQuery<T> {
    val changes = beginGroup().changed().or().attachmentChanged().or().deleted(true).endGroup()
    return changes.and().changedByUser()
}

fun <T> RealmQuery<T>.pageIndexUserChanges(
): RealmQuery<T> {
    return changedByUser().and().changed()
}

fun <T> RealmQuery<T>.userChanges(
): RealmQuery<T> {
    val changed = beginGroup().changed().or().deleted(true).endGroup()
    return changed.and().changedByUser()
}

fun <T> RealmQuery<T>.file(
    downloaded: Boolean
): RealmQuery<T> {
    return equalTo("fileDownloaded", downloaded)
}

fun <T> RealmQuery<T>.itemSearch(components: List<String>): RealmQuery<T> {
    var result = this
    for ((index, component) in components.withIndex()) {
        result = result.beginGroup()
        result = itemSearchSubpredicates(query = result, component)
        result = result.endGroup()
        if (index != components.size -1) {
            result = result.and()
        }
    }
    return result
}

private fun <T> itemSearchSubpredicates(query: RealmQuery<T>, text: String) : RealmQuery<T>  {
    var result = query
        .equalTo("key", text)
        .or()
        .equalTo("children.key", text)
        .or()
        .contains("displayTitle", text, Case.INSENSITIVE)
        .or()
        .contains("tags.tag.name", text, Case.INSENSITIVE)
        .or()
        .contains("children.tags.tag.name", text, Case.INSENSITIVE)
        .or()
        .contains("children.children.tags.tag.name", text, Case.INSENSITIVE)
        .or()
        .contains("creators.name", text, Case.INSENSITIVE)
        .or()
        .contains("creators.firstName", text, Case.INSENSITIVE)
        .or()
        .contains("creators.lastName", text, Case.INSENSITIVE)

    val int = text.toIntOrNull()
    if (int != null) {
        result = result.or().equalTo("parsedYear", int)
    }

    return result
}