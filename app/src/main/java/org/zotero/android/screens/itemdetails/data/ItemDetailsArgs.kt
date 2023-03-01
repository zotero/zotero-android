package org.zotero.android.screens.itemdetails.data

import org.zotero.android.sync.Library

data class ItemDetailsArgs(val type: DetailType, val library: Library, val childKey: String?) {
}