package org.zotero.android.architecture.database.objects

enum class UpdatableChangeType(val i: Int) {
    sync(0),
    user(1),
    syncResponse(2)
}