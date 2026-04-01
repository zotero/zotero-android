package org.zotero.android.screens.login.data

import android.content.Context
import org.zotero.android.uicomponents.Strings

sealed class LoginError: Exception() {
    data class serverError(val errorMessage: String): LoginError()
    object sessionTimedOut: LoginError()
    data class unknown(val error: Throwable): LoginError()

    fun localizedDescription(context: Context): String {
        return when (this) {
            is serverError -> {
                this.errorMessage
            }

            sessionTimedOut -> {
                context.getString(Strings.errors_login_session_timed_out)
            }

            is unknown -> {
                this.error.localizedMessage ?: ""
            }
        }
    }
}