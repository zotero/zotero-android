package org.zotero.android.database.migration.main.solutions

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.changed
import org.zotero.android.database.requests.key
import org.zotero.android.sync.AnnotationConverter
import timber.log.Timber

class MigrateNonArabicFormattingSortIndex(private val dynamicRealm: DynamicRealm) {
    fun migrate() {
        val allChangesItems = dynamicRealm.where(RItem::class.java.simpleName)
            .changed()
            .findAll()
        for (item in allChangesItems) {
            fixRItemSortIndex(item)
            fixRItemFieldSortIndex(item)
        }
    }

    private fun fixRItemSortIndex(item: DynamicRealmObject): Boolean {
        var wrongSortIndex: String? = null
        try {
            wrongSortIndex = item.getString("annotationSortIndex") ?: return false
            if (wrongSortIndex.isEmpty()) {
                return false
            }
            val splitted = wrongSortIndex.split("|")
            if (splitted.size != 3) {
                return false
            }
            val (wrongPageIndex: Int, wrongTextOffset: Int, wrongMinY: Int) = splitted.map { it.toInt() }
            val fixedSortIndex = AnnotationConverter.Companion.sortIndex(
                pageIndex = wrongPageIndex,
                textOffset = wrongTextOffset,
                minY = wrongMinY
            )
            if (wrongSortIndex != fixedSortIndex) {
                item.setString("annotationSortIndex", fixedSortIndex)
            }
            return true
        } catch (e: Exception) {
            Timber.e(
                e,
                "Unable to DB Migrate incorrect Sort Index = ${wrongSortIndex}"
            )
            return false
        }
    }


    private fun fixRItemFieldSortIndex(item: DynamicRealmObject) {
        val fields = item.getList("fields") ?: return
        val changedRFields = fields.where()
            .equalTo("changed", true)
            .key(FieldKeys.Item.Annotation.sortIndex)
            .findAll()
        for (rField in changedRFields) {
            var wrongSortIndex: String? = null
            try {
                wrongSortIndex = rField.getString("value") ?: continue
                if (wrongSortIndex.isEmpty()) {
                    continue
                }
                val splitted = wrongSortIndex.split("|")
                if (splitted.size != 3) {
                    continue
                }
                val (wrongPageIndex: Int, wrongTextOffset: Int, wrongMinY: Int) = splitted.map { it.toInt() }
                val fixedSortIndex = AnnotationConverter.Companion.sortIndex(
                    pageIndex = wrongPageIndex,
                    textOffset = wrongTextOffset,
                    minY = wrongMinY
                )
                if (wrongSortIndex != fixedSortIndex) {
                    rField.setString("value", fixedSortIndex)
                }
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Unable to DB Migrate incorrect Sort Index = ${wrongSortIndex}"
                )
            }
        }
    }
}