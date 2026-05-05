package org.zotero.android.database.migration.bundle

import io.realm.DynamicRealm
import io.realm.RealmMigration
import org.zotero.android.database.migration.bundle.solutions.MigrateAddRStyle

internal class BundleDbMigration(private val fileName: String) : RealmMigration {
    override fun migrate(dynamicRealm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion < 5) {
            MigrateAddRStyle(dynamicRealm).migrate()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BundleDbMigration

        return fileName == other.fileName
    }

    override fun hashCode(): Int {
        return fileName.hashCode()
    }

}