package org.zotero.android.database.objects

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import java.util.Date

open class RStyle : RealmObject(){

    @PrimaryKey
    var identifier: String = ""

    var title: String = ""
    var href: String = ""
    lateinit var updated: Date
    var filename: String = ""
    var dependency: RStyle? = null
    var installed: Boolean = false
    var supportsBibliography: Boolean = false
    var isNoteStyle: Boolean = false
    var defaultLocale: String = ""

    @LinkingObjects("dependency")
    val dependent: RealmResults<RStyle>? = null

    val id: String get() { return this.identifier }

}
