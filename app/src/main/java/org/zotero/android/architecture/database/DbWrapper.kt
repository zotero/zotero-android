package org.zotero.android.architecture.database

import RealmDbStorage
import org.zotero.android.files.FileStore

class DbWrapper constructor(private val fileStore: FileStore) {

    lateinit var realmDbStorage: RealmDbStorage

    fun initDb() {
        realmDbStorage = RealmDbStorage(config = Database.mainConfiguration(fileStore = fileStore))

    }
}