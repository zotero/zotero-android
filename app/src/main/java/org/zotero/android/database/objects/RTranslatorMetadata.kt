package org.zotero.android.database.objects

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.Date

open class RTranslatorMetadata : RealmObject() {
    @PrimaryKey
    var id: String = ""
    lateinit var lastUpdated: Date
}