package org.zotero.android.database

import io.realm.RealmConfiguration
import org.zotero.android.database.migration.bundle.BundleDbMigration
import org.zotero.android.database.migration.bundle.BundledDataConfigurationDbModule
import org.zotero.android.database.migration.main.MainConfigurationDbModule
import org.zotero.android.database.migration.main.MainDbMigration
import java.io.File

class Database {
    companion object {
        private const val schemaVersion = 6L //From now on must only increase by 1 whenever db schema changes

        fun mainConfiguration(dbFile: File): RealmConfiguration {
            val builder = RealmConfiguration.Builder()
                .directory(dbFile.parentFile!!)
                .name(dbFile.name)
                .modules(MainConfigurationDbModule())
                .schemaVersion(schemaVersion)
                .allowWritesOnUiThread(true)
                .migration(MainDbMigration(fileName = dbFile.name))

            return builder.build()
        }

        fun bundledDataConfiguration(dbFile: File): RealmConfiguration {
            val builder = RealmConfiguration.Builder()
                .directory(dbFile.parentFile!!)
                .name(dbFile.name)
                .modules(BundledDataConfigurationDbModule())
                .schemaVersion(schemaVersion)
                .allowWritesOnUiThread(true)
                .migration(BundleDbMigration(fileName = dbFile.name))
            return builder.build()
        }
    }
}
