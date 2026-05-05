package org.zotero.android.database.migration.main

import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.zotero.android.database.migration.main.solutions.MigrateAllItemsDbRowTypeIconNameTypeChange
import org.zotero.android.database.migration.main.solutions.MigrateExtractAnnotationTypeFromItems
import org.zotero.android.database.migration.main.solutions.MigrateMarkAllNonLocalGroupsAsOutdatedToTriggerResync
import org.zotero.android.database.migration.main.solutions.MigrateNonArabicFormattingSortIndex

internal class MainDbMigration(private val fileName: String) : RealmMigration {

    override fun migrate(dynamicRealm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion < 2) {
            MigrateAllItemsDbRowTypeIconNameTypeChange(dynamicRealm).migrate()
        }
        if (oldVersion < 3) {
            MigrateMarkAllNonLocalGroupsAsOutdatedToTriggerResync(dynamicRealm).migrate()
        }
        if (oldVersion < 4) {
            MigrateExtractAnnotationTypeFromItems(dynamicRealm).migrate()
        }
        if (oldVersion < 6) {
            MigrateNonArabicFormattingSortIndex(dynamicRealm).migrate()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MainDbMigration

        return fileName == other.fileName
    }

    override fun hashCode(): Int {
        return fileName.hashCode()
    }

}