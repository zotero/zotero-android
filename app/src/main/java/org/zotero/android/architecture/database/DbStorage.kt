package org.zotero.android.architecture.database
import io.realm.Realm
import kotlin.reflect.KClass

enum class DbError {
    objectNotFound,
    primaryKeyUnavailable,
    invalidRequest;

    val isObjectNotFound: Boolean
        get() {
            return when (this) {
                objectNotFound -> true
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