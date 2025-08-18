package org.zotero.android.sync

enum class LinkMode(val str: String) {
    linkedFile("linked_file"),
    importedFile("imported_file"),
    linkedUrl("linked_url"),
    importedUrl("imported_url"),
    embeddedImage("embedded_image");

    companion object {
        private val map = entries.associateBy(LinkMode::str)
        fun from(str: String) = map[str]
    }
}
