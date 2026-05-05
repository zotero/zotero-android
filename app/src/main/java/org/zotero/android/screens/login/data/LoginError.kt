package org.zotero.android.screens.login.data

sealed class LoginError: Exception() {
    data class serverError(val errorMessage: String): LoginError()
    data class unknown(val error: Throwable): LoginError()

    val localizedDescription: String get() {
        return when (this) {
            is serverError -> {
                this.errorMessage
            }

            is unknown -> {
                this.error.localizedMessage ?: ""
            }
        }
    }
}