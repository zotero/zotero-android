package org.zotero.android.architecture.database

import io.realm.RealmConfiguration
import org.zotero.android.architecture.database.objects.RCustomLibrary
import org.zotero.android.architecture.database.objects.RGroup
import org.zotero.android.architecture.database.objects.RVersions
import org.zotero.android.files.FileStore

class Database {
    companion object {
        private val schemaVersion: Long = 34

        fun mainConfiguration(fileStore: FileStore): RealmConfiguration {
            return RealmConfiguration.Builder(setOf(RVersions::class, RGroup::class, RCustomLibrary::class))
                .directory(fileStore.getRootDirectory().absolutePath)
                .name(fileStore.getDbFile().name)
                .schemaVersion(schemaVersion)
                .build()
        }
    }
}