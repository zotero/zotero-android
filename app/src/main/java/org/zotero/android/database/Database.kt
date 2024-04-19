package org.zotero.android.database

import io.realm.RealmConfiguration
import org.zotero.android.files.FileStore
import java.io.File

class Database {
    companion object {
        private const val schemaVersion = 1L //From now on must only increase by 1 whenever db schema changes

        fun mainConfiguration(dbFile: File): RealmConfiguration {
            val builder = RealmConfiguration.Builder()
                .directory(dbFile.parentFile!!)
                .name(dbFile.name)
                .schemaVersion(schemaVersion)
                .allowWritesOnUiThread(true)
                .deleteRealmIfMigrationNeeded()

            return builder.build()
        }

        fun bundledDataConfiguration(fileStorage: FileStore): RealmConfiguration {
            val dbFile = fileStorage.bundledDataDbFile()
            val builder = RealmConfiguration.Builder()
                .directory(dbFile.parentFile!!)
                .name(dbFile.name)
                .schemaVersion(schemaVersion)
                .allowWritesOnUiThread(true)
                .deleteRealmIfMigrationNeeded()

            return builder.build()
        }
    }
}