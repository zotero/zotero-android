package org.zotero.android.sync

import org.zotero.android.database.objects.RTag
import org.zotero.android.database.objects.RTypedTag

data class Tag(
    val name: String,
    val color: String,
    val type: RTypedTag.Kind
) {

    val id: String get() { return this.name }

    constructor(name: String, color: String) : this(
        name = name, color = color, type = RTypedTag.Kind.manual
    )

    constructor(tag: RTag): this(name = tag.name, color = tag.color, type = RTypedTag.Kind.manual)

    constructor(tag: RTypedTag): this(
        name = tag.tag?.name ?: "",
        color = tag.tag?.color ?: "",
        type = RTypedTag.Kind.valueOf(tag.type)
    )
}