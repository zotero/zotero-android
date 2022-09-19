package org.zotero.android.architecture.database.objects

import io.realm.RealmObject
import io.realm.annotations.RealmClass

@RealmClass(embedded = true)
open class RLink: RealmObject() {
    var type: String = ""
    var href: String= ""
    var contentType: String= ""
    var title: String= ""
    var length: Int = 0

    val linkType: LinkType get(){
        return LinkType.valueOf(type)
    }
}
