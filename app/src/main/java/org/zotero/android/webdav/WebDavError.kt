package org.zotero.android.webdav

sealed class WebDavError {
    sealed class Upload : Exception() {
        object cantCreatePropData : Upload()
        // TODO network error
        data class apiError(val error: Exception , val httpMethod: String?): Upload()
    }
}
