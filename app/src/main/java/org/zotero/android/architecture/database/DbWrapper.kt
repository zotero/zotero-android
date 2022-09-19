package org.zotero.android.architecture.database

import android.content.Context
import org.zotero.android.files.FileStore

class DbWrapper constructor(private val fileStore: FileStore) {

    lateinit var realmDbStorage: RealmDbStorage

    fun initDb(context: Context) {
        realmDbStorage = RealmDbStorage(config = Database.mainConfiguration(fileStore = fileStore, context = context))

    }
}