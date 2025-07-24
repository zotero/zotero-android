package org.zotero.android.citation

interface CitationControllerInterface {

    fun getLocator(): String
    fun getLocatorValue(): String
    fun omitAuthor(): Boolean
}