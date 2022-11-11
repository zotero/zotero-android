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

        fun correctedModifications(modifications: IntArray, insertions: IntArray, deletions: IntArray): IntArray {
            if (modifications.isEmpty() || !(!insertions.isEmpty() || !deletions.isEmpty())) {
                return modifications
            }
            var correctedModifications = modifications.toMutableList()

            deletions.forEach { deletion ->
                val deletionIdx = modifications.indexOfFirst { it > deletion }
                if (deletionIdx != -1) {
                    for (idx in deletionIdx..modifications.size) {
                        correctedModifications[idx] -= 1
                    }
                }
            }

            val modifications = correctedModifications

            insertions.forEach { insertion ->
                val insertionIdx = modifications.indexOfFirst { it >= insertion }
                if (insertionIdx != -1) {
                    for (idx in insertionIdx..modifications.size) {
                        correctedModifications[idx] += 1
                    }
                }
            }

            return correctedModifications.toIntArray()

        }
    }
}