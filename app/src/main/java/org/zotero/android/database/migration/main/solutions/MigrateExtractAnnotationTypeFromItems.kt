package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.key

class MigrateExtractAnnotationTypeFromItems(private val dynamicRealm: DynamicRealm) {

    fun migrate() {
        val realmSchema = dynamicRealm.schema

        val rItemDbSchema = realmSchema.get(RItem::class.java.simpleName)
        rItemDbSchema?.run {
            addField("annotationType", String::class.java, FieldAttribute.REQUIRED)
            transform {
                it.set("annotationType", "")
            }
        }

        val allItems = dynamicRealm.where(RItem::class.java.simpleName).findAll()
        for (item in allItems) {
            val rawType = item.getString("rawType") ?: continue
            val fields = item.getList("fields") ?: continue
            when (rawType) {
                ItemTypes.annotation -> {
                    val annotationType = fields.where().key(
                        FieldKeys.Item.Annotation.type
                    ).findFirst()?.getString("value") ?: continue
                    if (annotationType.isEmpty()) {
                        continue
                    }
                    item.setString("annotationType", annotationType)
                }
            }
        }
    }
}