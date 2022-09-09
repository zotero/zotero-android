package org.zotero.android.architecture.database.objects

enum class LinkType(val str: String) {
    me("self"),
    alternate("alternate"),
    up("up"),
    enclosure("enclosure")
}
