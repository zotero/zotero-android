import io.realm.kotlin.Realm

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

interface DbResponseRequest<T> {
    val needsWrite: Boolean
    fun process(database: Realm): T
}