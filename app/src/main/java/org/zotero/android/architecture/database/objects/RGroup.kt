package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

enum class GroupType(val str: String) {
    privateOpt("Private"),
    publicOpen("PublicOpen"),
    publicClosed("PublicClosed"),
}

open class RGroup : RealmObject() {
    @PrimaryKey
    var identifier: Int = 0
    var owner: Int = 0
    var name: String = ""
    var desc: String = ""
    var type: String = GroupType.privateOpt.str
    var canEditMetadata: Boolean = false
    var canEditFiles: Boolean = false
    var orderId: Int = 0
    var versions: RVersions? = null

    var isLocalOnly: Boolean = false
    var version: Int = 0
    lateinit var syncState: String //ObjectSyncState

    val isInvalidated: Boolean
        get() = !isValid
}
