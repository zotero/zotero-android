package org.zotero.android.database

import io.realm.RealmConfiguration
import org.zotero.android.files.FileStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbWrapperBundle @Inject constructor(
    private val fileStore: FileStore
) {

    lateinit var realmDbStorage: RealmDbStorage
    lateinit var config: RealmConfiguration

    fun initBundleDataConfiguration() {
        val dbFile = fileStore.bundledDataDbFile()
        this.config = Database.bundledDataConfiguration(dbFile)
        this.realmDbStorage = RealmDbStorage(config)
    }
}