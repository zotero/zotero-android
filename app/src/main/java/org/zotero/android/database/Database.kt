package org.zotero.android.database

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.File

class Database {
    companion object {
        private const val schemaVersion = 1L //From now on must only increase by 1 whenever db schema changes

        fun mainConfiguration(dbFile: File, context: Context): RealmConfiguration {
            Realm.init(context)
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