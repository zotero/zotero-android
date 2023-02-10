package org.zotero.android.database

import android.content.Context
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber
import java.io.File

class DbWrapper constructor() {

    lateinit var realmDbStorage: RealmDbStorage
    lateinit var config: RealmConfiguration
    var isInitialized = false

    fun initWithMainConfiguration(context: Context, dbFile: File) {
        config = org.zotero.android.database.Database.mainConfiguration(dbFile = dbFile, context = context)
        realmDbStorage = RealmDbStorage(config = config)
        isInitialized = true
    }

    fun clearDatabaseFiles() {
        val realmUrl = config.path ?: return
        Realm.deleteRealm(config)
        val realmUrls = arrayOf(
            realmUrl,
            "$realmUrl.lock",
            "$realmUrl.note",
            "$realmUrl.management"
        )

        for (url in realmUrls) {
            val file = File(url)
            val wasDeleted = file.deleteRecursively()
            if (file.exists() && !wasDeleted) {
                Timber.e("FileManager: couldn't delete file at $url")
            }
        }
    }
}