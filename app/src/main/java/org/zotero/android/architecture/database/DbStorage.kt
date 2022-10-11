package org.zotero.android.architecture.database
import io.realm.Realm
import kotlin.reflect.KClass

sealed class DbError: Throwable() {
    object objectNotFound: DbError()
    object primaryKeyUnavailable: DbError()
    object invalidRequest: DbError();

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

interface DbResponseRequest<A : Any, B: Any> {
    val needsWrite: Boolean
    fun process(database: Realm, clazz: KClass<A>? = null): B
}