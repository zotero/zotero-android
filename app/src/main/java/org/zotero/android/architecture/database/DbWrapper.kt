package org.zotero.android.architecture.database

import android.content.Context
import java.io.File

class DbWrapper constructor() {

    lateinit var realmDbStorage: RealmDbStorage
    var isInitialized = false

    fun initWithMainConfiguration(context: Context, dbFile: File) {
        realmDbStorage = RealmDbStorage(config = Database.mainConfiguration(dbFile = dbFile, context = context))
        isInitialized = true

    }
}