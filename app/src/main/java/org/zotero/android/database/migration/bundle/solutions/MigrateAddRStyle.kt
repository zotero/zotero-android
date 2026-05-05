package org.zotero.android.database.migration.bundle.solutions

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import java.util.Date

class MigrateAddRStyle(private val dynamicRealm: DynamicRealm) {

    fun migrate() {
        val realmSchema = dynamicRealm.schema
        realmSchema.create("RStyle")
            .addField("identifier", String::class.java, FieldAttribute.PRIMARY_KEY)
            .setRequired("identifier", true)
            .addField("title", String::class.java, FieldAttribute.REQUIRED)
            .addField("href", String::class.java, FieldAttribute.REQUIRED)
            .addField("updated", Date::class.java)
            .addField("filename", String::class.java, FieldAttribute.REQUIRED)
            .addRealmObjectField("dependency", realmSchema.get("RStyle")!!)
            .addField("installed", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("supportsBibliography", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("isNoteStyle", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("defaultLocale", String::class.java, FieldAttribute.REQUIRED)
    }

}