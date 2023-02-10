package org.zotero.android.api.pojo.sync

import org.zotero.android.database.objects.RTypedTag

data class TagResponse(
    val tag: String,
    val type: RTypedTag.Kind,
)
