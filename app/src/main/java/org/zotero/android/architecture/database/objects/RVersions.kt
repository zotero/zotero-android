package org.zotero.android.architecture.database.objects

import io.realm.kotlin.types.EmbeddedRealmObject

class RVersions : EmbeddedRealmObject {
    var collections: Int = 0
    var items: Int = 0
    var trash: Int = 0
    var searches: Int = 0
    var deletions: Int = 0
    var settings: Int = 0


}

