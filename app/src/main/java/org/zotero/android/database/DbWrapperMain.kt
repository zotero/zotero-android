package org.zotero.android.database

import io.realm.Realm
import io.realm.RealmConfiguration
import org.zotero.android.files.FileStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DbWrapperMain @Inject constructor(
    private val fileStore: FileStore
) {
    lateinit var realmDbStorage: RealmDbStorage
    lateinit var config: RealmConfiguration
    var isInitialized = false

    fun initWithMainConfiguration(userId: Long) {
        val dbFile = fileStore.dbFile(userId)
        this.config = Database.mainConfiguration(dbFile = dbFile)
        this.realmDbStorage = RealmDbStorage(config = config)
        isInitialized = true
    }

    fun clearDatabaseFiles() {
        val realmInstance = Realm.getInstance(config)
        realmInstance.removeAllChangeListeners()
        realmInstance.executeTransaction {
            realmInstance.deleteAll()
        }
        realmInstance.close()
    }
}