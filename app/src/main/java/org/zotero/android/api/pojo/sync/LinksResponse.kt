package org.zotero.android.api.pojo.sync

data class LinksResponse(
    val itself: LinkResponse?,
    val alternate: LinkResponse?,
    val up: LinkResponse?,
    val enclosure: LinkResponse?,
)
