package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.objects.RCollection
import org.zotero.android.sync.LibraryIdentifier

// Keys of the collection and all of its subcollections, at any depth
fun Realm.selfAndSubcollectionKeys(key: String, libraryId: LibraryIdentifier): Set<String> {
    val keys = mutableSetOf(key)
    val children = where<RCollection>().parentKey(key, libraryId).findAll()
    for (child in children) {
        keys.addAll(selfAndSubcollectionKeys(child.key, libraryId))
    }
    return keys
}
