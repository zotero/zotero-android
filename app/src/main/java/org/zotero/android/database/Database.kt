package org.zotero.android.database

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.File

class Database {
    companion object {
        private const val schemaVersion = 39L

        fun mainConfiguration(dbFile: File, context: Context): RealmConfiguration {
            Realm.init(context)
            val builder = RealmConfiguration.Builder()
                .directory(dbFile.parentFile!!)
                .name(dbFile.name)
                .schemaVersion(schemaVersion)
                .deleteRealmIfMigrationNeeded()

            return builder.build()
        }

        fun correctedModifications(modifications: IntArray, insertions: IntArray, deletions: IntArray): IntArray {
            if (modifications.isEmpty() || !(!insertions.isEmpty() || !deletions.isEmpty())) {
                return modifications
            }
            var correctedModifications = modifications.toMutableList()

            deletions.forEach { deletion ->
                val deletionIdx = modifications.indexOfFirst { it > deletion }
                if (deletionIdx != -1) {
                    for (idx in deletionIdx until modifications.size) {
                        correctedModifications[idx] -= 1
                    }
                }
            }

            val modifications = correctedModifications.toMutableList()

            insertions.forEach { insertion ->
                val insertionIdx = modifications.indexOfFirst { it >= insertion }
                if (insertionIdx != -1) {
                    for (idx in insertionIdx until modifications.size) {
                        correctedModifications[idx] += 1
                    }
                }
            }

            return correctedModifications.toIntArray()

        }
    }
}