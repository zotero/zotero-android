package org.zotero.android.api.pojo.sync

data class SearchResponse(
    val key: String,
    val library: LibraryResponse,
    val links: LinksResponse?,
    val data: SearchResponse.Data,
    val version: Int,
) {

    data class Data(
        val name: String,
        val conditions: List<ConditionResponse>,
        val isTrash: Boolean
    )
}

data class ConditionResponse(
    val condition: String,
    val operator: String,
    val value: String
)
