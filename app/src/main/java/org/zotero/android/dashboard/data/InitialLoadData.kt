package org.zotero.android.dashboard.data

import org.zotero.android.sync.Library
import org.zotero.android.uidata.Collection

data class InitialLoadData(
    val collection: Collection,
    val library: Library
) {
}