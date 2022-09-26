package org.zotero.android.sync

enum class LinkMode(str: String) {
    linkedFile("linked_file"),
    importedFile("imported_file"),
    linkedUrl("linked_url"),
    importedUrl("imported_url"),
    embeddedImage("embedded_image"),
}
