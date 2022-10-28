package org.zotero.android.architecture.database

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import org.zotero.android.files.FileStore

class Database {
    companion object {
        private val schemaVersion: Long = 35

        fun mainConfiguration(fileStore: FileStore, context: Context): RealmConfiguration {
            Realm.init(context)
            val builder = RealmConfiguration.Builder()
                .directory(fileStore.getRootDirectory())
                .name(fileStore.getDbFile().name)
                .schemaVersion(schemaVersion)
                .deleteRealmIfMigrationNeeded()

            return builder.build()

//            return RealmConfiguration.Builder(setOf(RVersions::class, RGroup::class, RCustomLibrary::class))
//
//                .build()
        }
    }
}