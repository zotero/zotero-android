package org.zotero.android.translator.data

import com.google.gson.JsonArray

sealed interface TranslatorAction {
    data class loadedItems(
        val data: JsonArray,
        val cookies: String?,
        val userAgent: String?,
        val referrer: String?
    ) : TranslatorAction

    data class selectItem(val data: List<Pair<String, String>>) : TranslatorAction
    data class reportProgress(val progress: String) : TranslatorAction
}