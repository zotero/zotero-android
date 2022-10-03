package org.zotero.android.architecture.database
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

enum class RealmDbError {
    autocreateMissingPrimaryKey
}

class RealmDbStorage(val config: RealmConfiguration) {

    val willPerformBetaWipe: Boolean
        get() {
            return config.shouldDeleteRealmIfMigrationNeeded()
        }

    fun clear() {
        val realmUrl = config.path ?: return

        val realmUrls = arrayOf(
            realmUrl,
            "$realmUrl.lock",
            "$realmUrl.note",
            "$realmUrl.management"
        )

        for (url in realmUrls) {
            val result = File(url).delete()
            if (!result) {
                Timber.e("org.zotero.android.architecture.database.RealmDbStorage: couldn't delete file at $url")
            }
        }
    }

    suspend fun perform(coordinatorAction: (RealmDbCoordinator) -> Unit) =
        withContext(Dispatchers.IO) {
            val coordinator = RealmDbCoordinator().init(config)
            coordinatorAction(coordinator)
        }

    suspend inline fun <reified T : Any> perform(request: DbResponseRequest<T, T>) = withContext(Dispatchers.IO) {
        return@withContext perform(request = request, invalidateRealm = false, refreshRealm = false)
    }

    suspend inline fun <reified T : Any> perform(request: DbResponseRequest<T, T>, refreshRealm: Boolean) =
        withContext(Dispatchers.IO) {
            return@withContext perform(
                request = request,
                invalidateRealm = false,
                refreshRealm = refreshRealm
            )
        }

    suspend inline fun <reified T: Any> perform(
        request: DbResponseRequest<T, T>,
        invalidateRealm: Boolean,
        q: String = ""
    ) =
        withContext(Dispatchers.IO) {
            return@withContext perform(
                request = request,
                invalidateRealm = invalidateRealm,
                refreshRealm = false
            )
        }

    suspend inline fun <reified T: Any> perform(
        request: DbResponseRequest<T, T>,
        invalidateRealm: Boolean,
        refreshRealm: Boolean
    ): T {
        val coordinator = RealmDbCoordinator().init(config)

        if (refreshRealm) {
            coordinator.refresh()
        }
        val result = coordinator.perform(request = request)

        if (invalidateRealm) {
            coordinator.invalidate()
        }

        return result
    }

    suspend fun perform(request: DbRequest) = withContext(Dispatchers.IO) {
        val coordinator = RealmDbCoordinator().init(config)
        coordinator.perform(request = request)
    }

    suspend fun perform(requests: List<DbRequest>) = withContext(Dispatchers.IO) {
        val coordinator = RealmDbCoordinator().init(config)
        coordinator.perform(requests)
    }
}

class RealmDbCoordinator {
    lateinit var realm: Realm

    fun init(configuration: RealmConfiguration): RealmDbCoordinator {
        this.realm = Realm.getInstance(configuration)
        return this
    }

    fun perform(request: DbRequest) {
        if (!request.needsWrite) {
            request.process(realm)
            return
        }


        realm.executeTransaction {
            request.process(realm)
        }
    }

    inline fun <reified T: Any> perform(request: DbResponseRequest<T, T>): T {
        if (!request.needsWrite) {
            return request.process(realm, T::class)
        }

        return request.process(realm, T::class)

    }

    suspend fun perform(requests: List<DbRequest>) = withContext(Dispatchers.IO) {

        realm.executeTransaction {
            for (request in requests) {
                if (!request.needsWrite) {
                    continue
                }

                request.process(realm)
            }
        }
    }

    fun refresh() {
        realm.refresh()
    }

    fun invalidate() {
//        realm.invalidate()
    }


}
