package org.zotero.android.sync.syncactions.data

sealed class ZoteroApiError: Exception() {
    object unchanged: ZoteroApiError()
    data class responseMissing(val str: String): ZoteroApiError()
}