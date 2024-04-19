package org.zotero.android.database

import io.realm.Realm
import io.realm.RealmConfiguration
import org.zotero.android.files.FileStore
import java.io.File

class DbWrapper constructor() {

    lateinit var realmDbStorage: RealmDbStorage
    lateinit var config: RealmConfiguration
    var isInitialized = false

    fun initWithMainConfiguration(dbFile: File) {
        config = Database.mainConfiguration(dbFile = dbFile)
        realmDbStorage = RealmDbStorage(config = config)
        isInitialized = true
    }

    fun initBundleDataConfiguration(fileStorage: FileStore) {
        config = Database.bundledDataConfiguration(fileStorage)
        realmDbStorage = RealmDbStorage(config = config)
        isInitialized = true
    }

    fun clearDatabaseFiles() {
        val realmInstance = Realm.getInstance(config)
        realmInstance.removeAllChangeListeners()
        realmInstance.executeTransaction {
            realmInstance.deleteAll()
        }
        println()
//        val realmUrls = arrayOf(
//            realmUrl,
//            "$realmUrl.lock",
//            "$realmUrl.note",
//            "$realmUrl.management"
//        )
//
//        for (url in realmUrls) {
//            val file = File(url)
//            val wasDeleted = file.deleteRecursively()
//            if (file.exists() && !wasDeleted) {
//                Timber.e("FileManager: couldn't delete file at $url")
//            }
//        }
    }
}