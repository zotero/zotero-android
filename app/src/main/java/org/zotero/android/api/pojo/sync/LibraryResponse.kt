package org.zotero.android.api.pojo.sync

data class LibraryResponse(val id: Int,
                           val name: String,
                           val type: String,
                           val links: LinksResponse?) {
}