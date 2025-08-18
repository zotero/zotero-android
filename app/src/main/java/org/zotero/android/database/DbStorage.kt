package org.zotero.android.database
import io.realm.Realm

sealed class DbError: Throwable() {
    object objectNotFound: DbError()
    object primaryKeyUnavailable: DbError()
    object invalidRequest: DbError()

    val isObjectNotFound: Boolean
        get() {
            return when (this) {
                is objectNotFound -> true
                else -> false
            }
        }
}


interface DbRequest {
    val needsWrite: Boolean

    fun process(database: Realm)
}

interface DbResponseRequest<T> {
    val needsWrite: Boolean
    fun process(database: Realm): T
}