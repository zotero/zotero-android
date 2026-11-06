package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import org.zotero.android.database.objects.RItem

class MigrateSpeechLanguage(private val dynamicRealm: DynamicRealm) {
    fun migrate() {
        dynamicRealm.schema.get(RItem::class.java.simpleName)?.run {
            addField("speechLanguage", String::class.java)
        }
    }
}