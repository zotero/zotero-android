package org.zotero.android.translator.data

sealed interface TranslatorAction {
    data class loadedItems(
        val data: List<Pair<String, Any>>,
        val cookies: String?,
        val userAgent: String?,
        val referrer: String?
    ) : TranslatorAction

    data class selectItem(val params: List<Pair<String, String>>) : TranslatorAction
    data class reportProgress(val str: String) : TranslatorAction
}