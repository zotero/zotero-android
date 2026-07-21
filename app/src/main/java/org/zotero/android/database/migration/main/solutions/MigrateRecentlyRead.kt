package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmSchema
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RPageIndex
import java.util.Date

class MigrateRecentlyRead(private val dynamicRealm: DynamicRealm) {
    fun migrate() {
        val realmSchema = dynamicRealm.schema

        createRLastReadDateSchema(realmSchema)
        updateRItemDbSchema(realmSchema)
        updateRPageIndex(realmSchema)
    }

    private fun createRLastReadDateSchema(realmSchema: RealmSchema) {
        realmSchema.create("RLastReadDate")
            .addField("key", String::class.java, FieldAttribute.REQUIRED, FieldAttribute.INDEXED)
            .addField("date", Date::class.java)
            .addField("changed", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("groupKey", Int::class.java).setNullable("groupKey", true)
            .addRealmListField("changes", realmSchema.get("RObjectChange")!!)
            .addField("version", Int::class.java, FieldAttribute.INDEXED)
            .addField("syncState", String::class.java)
            .addField("lastSyncDate", Date::class.java)
            .addField("syncRetries", Int::class.java, FieldAttribute.REQUIRED)
            .addField("deleted", Boolean::class.java, FieldAttribute.REQUIRED)
            .addField("changeType", String::class.java)
    }

    private fun updateRItemDbSchema(realmSchema: RealmSchema) {
        val rItemDbSchema = realmSchema.get(RItem::class.java.simpleName)

        rItemDbSchema?.run {
            addField("lastRead", Date::class.java, FieldAttribute.INDEXED)
            addField("effectiveLastRead", Date::class.java)
            transform {
                it.setNull("lastRead")
                it.setNull("effectiveLastRead")
            }
        }
    }

    private fun updateRPageIndex(realmSchema: RealmSchema) {
        val rPageIndexSchema = realmSchema.get(RPageIndex::class.java.simpleName)

        rPageIndexSchema?.run {
            addField("deleted", Boolean::class.java)
            transform {
                it.setBoolean("deleted", false)
            }
        }
    }

}