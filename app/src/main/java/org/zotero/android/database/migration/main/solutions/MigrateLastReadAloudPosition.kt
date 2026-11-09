package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import java.util.Date

class MigrateLastReadAloudPosition(private val dynamicRealm: DynamicRealm) {
    fun migrate() {
        val realmSchema = dynamicRealm.schema
        realmSchema.create("RLastReadAloudPosition")
            .addField("key", String::class.java, FieldAttribute.REQUIRED, FieldAttribute.INDEXED)
            .addField("pageIndex", Int::class.java).setNullable("pageIndex", true)
            .addRealmListField("rects", realmSchema.get("RRect")!!)
            .addField("sourceJson", String::class.java)
            .addField("customLibraryKey", String::class.java)
            .addField("groupKey", Int::class.java).setNullable("groupKey", true)
            .addField("version", Int::class.java, FieldAttribute.INDEXED)
            .addField("syncState", String::class.java)
            .addField("lastSyncDate", Date::class.java)
            .addField("syncRetries", Int::class.java, FieldAttribute.REQUIRED)
            .addRealmListField("changes", realmSchema.get("RObjectChange")!!)
            .addField("changeType", String::class.java)
            .addField("deleted", Boolean::class.java, FieldAttribute.REQUIRED)
    }
}