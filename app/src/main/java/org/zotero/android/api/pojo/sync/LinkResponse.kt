package org.zotero.android.api.pojo.sync

data class LinkResponse(
    val href: String,
    val type: String?,
    val title: String?,
    val length: Int?
)
