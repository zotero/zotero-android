package org.zotero.android.architecture

import timber.log.Timber
import org.zotero.android.architecture.Result.Failure
import org.zotero.android.architecture.Result.Success

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()

    // We use Failure instead of Error as a name for this class since Error clashes easily
    // with kotlin.Error typealias when typing in AS
    data class Failure(val exception: Exception) : Result<Nothing>()
}

inline fun <T, R> Result<T>.mapSuccess(block: (T) -> R): Result<R> = when (this) {
    is Success -> Success(block(this.value))
    is Failure -> Failure(this.exception)
}

suspend fun <T, R> Result<T>.coMapSuccess(block: suspend (T) -> R): Result<R> = when (this) {
    is Success -> Success(block(this.value))
    is Failure -> Failure(this.exception)
}

inline fun <T> Result<T>.mapFailure(block: (Exception) -> Exception): Result<T> = when (this) {
    is Success -> Success(this.value)
    is Failure -> Failure(block(this.exception))
}

suspend fun <T, R> Result<T>.flatMapSuccess(block: suspend (T) -> Result<R>): Result<R> =
    when (this) {
        is Success -> block(this.value)
        is Failure -> Failure(this.exception)
    }

inline fun <T> Result<T>.ifFailure(block: (Exception) -> Nothing): T {
    return when (this) {
        is Success -> this.value
        is Failure -> block(this.exception)
    }
}

fun <T> Result<T>.orNull(): T? = when (this) {
    is Success -> this.value
    is Failure -> null
}

fun <T> T.toSuccess(): Success<T> = Success(this)

fun Result<Any?>.mapSuccessUnit(): Result<Unit> = when (this) {
    is Success -> Success(Unit)
    is Failure -> Failure(this.exception)
}

fun <T> Result<T>.logFailure() = mapFailure {
    Timber.e(it)
    it
}
