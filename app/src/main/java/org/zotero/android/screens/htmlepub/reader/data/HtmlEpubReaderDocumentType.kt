package org.zotero.android.screens.htmlepub.reader.data

enum class HtmlEpubReaderDocumentType {
    EPUB,
    SNAPSHOT;

    companion object {
        fun fromContentType(contentType: String): HtmlEpubReaderDocumentType {
            return when (contentType) {
                "application/epub+zip" -> EPUB
                else -> SNAPSHOT
            }
        }
    }
}
