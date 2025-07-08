package org.zotero.android.styles.data

import java.util.Date

data class RemoteStyle(
    val title: String,
    val name: String,
    val dependent: Boolean,
    val category: RemoteCitationStyleCategory,
    val updated: Date,
    val href: String,
) {

    val id: String get() {
        return "http://zotero.org/styles/${this.name}"
    }
}