package org.zotero.android.database.objects

enum class UpdatableChangeType(val i: Int) {
    sync(0),
    user(1),
    syncResponse(2)
}