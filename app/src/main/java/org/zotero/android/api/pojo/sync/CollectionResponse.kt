package org.zotero.android.api.pojo.sync

data class CollectionResponse(
    val key: String,
    val library: LibraryResponse,
    val links: LinksResponse?,
    val data: Data,
    val version: Int
) {

    data class Data(
        val name: String,
        val parentCollection: String?,
        val isTrash: Boolean
    )
}