package org.zotero.android.api.pojo.sync

data class DeletionsResponse(
    val collections: List<String>,
    val searches: List<String>,
    val items: List<String>,
    val tags: List<String>,
)
