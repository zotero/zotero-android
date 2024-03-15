package org.zotero.android.api.pojo.sync

import org.zotero.android.database.objects.RTypedTag

data class TagResponse(
    val tag: String,
    val type: RTypedTag.Kind,
) {
    val automaticCopy: TagResponse get (){
        return TagResponse(tag = this.tag, type = RTypedTag.Kind.automatic)
    }
}
