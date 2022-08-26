package org.zotero.android.api.network

sealed class NetworkResultWrapper<out T> {
    data class Success<out T>(val value: T): NetworkResultWrapper<T>()
    data class NetworkError(val error: CustomNetworkError, val shouldRetry: Boolean): NetworkResultWrapper<Nothing>()
}