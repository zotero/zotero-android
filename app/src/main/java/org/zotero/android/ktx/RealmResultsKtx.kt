package org.zotero.android.ktx

import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.Sort
import org.zotero.android.database.requests.key
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber

inline fun <E : RealmObject> RealmResults<E>.uniqueObject(
    key: String,
    libraryId: LibraryIdentifier
): E? {
    val filtered = where().key(key, libraryId).findAll()
    if (filtered.isEmpty()) {
        return null
    }

    if (filtered.size == 1) {
        return filtered.first()
    }

    Timber.e(" $key; $libraryId contains more than 1 instance!")

    val sorted = filtered.sort("version", Sort.DESCENDING)

    val database = realm
    if (database != null && database.isInTransaction) {
        for (idx in (1..<sorted.size).reversed()) {
            sorted[idx]?.deleteFromRealm()
        }
    }
    return sorted.first()
}

inline fun <E : RealmObject> RealmQuery<E>.uniqueObjectV2(
    key: String,
    libraryId: LibraryIdentifier
): E? {
    val filtered = key(key, libraryId)
    if (filtered.count() == 0L) {
        return null
    }

    if (filtered.count() == 1L) {
        return filtered.findFirst()
    }

    Timber.e(" $key; $libraryId contains more than 1 instance!")

    val sorted = filtered.sort("version", Sort.DESCENDING).findAll()

    val database = realm
    if (database != null && database.isInTransaction) {
        for (idx in (1..<sorted.size).reversed()) {
            sorted[idx]?.deleteFromRealm()
        }
    }
    return sorted.first()
}