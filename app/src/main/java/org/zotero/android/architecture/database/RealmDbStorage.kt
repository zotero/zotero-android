import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

enum class RealmDbError {
    autocreateMissingPrimaryKey
}

class RealmDbStorage(private val config: RealmConfiguration) {

    val willPerformBetaWipe: Boolean
        get() {
            return config.deleteRealmIfMigrationNeeded
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
                Timber.e("RealmDbStorage: couldn't delete file at $url")
            }
        }
    }

    suspend fun perform(coordinatorAction: (RealmDbCoordinator) -> Unit) =
        withContext(Dispatchers.IO) {
            val coordinator = RealmDbCoordinator().init(config)
            coordinatorAction(coordinator)
        }

    suspend fun <T> perform(request: DbResponseRequest<T>) = withContext(Dispatchers.IO) {
        return@withContext perform(request = request, invalidateRealm = false, refreshRealm = false)
    }

    suspend fun <T> perform(request: DbResponseRequest<T>, refreshRealm: Boolean) =
        withContext(Dispatchers.IO) {
            return@withContext perform(
                request = request,
                invalidateRealm = false,
                refreshRealm = refreshRealm
            )
        }

    suspend fun <T> perform(
        request: DbResponseRequest<T>,
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

    suspend fun <T> perform(
        request: DbResponseRequest<T>,
        invalidateRealm: Boolean,
        refreshRealm: Boolean
    ): T {
        val coordinator = RealmDbCoordinator().init(config)
        val result = coordinator.perform(request = request)
        println(result)
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
    private lateinit var realm: Realm

    fun init(configuration: RealmConfiguration): RealmDbCoordinator {
        this.realm = Realm.open(configuration)
        return this
    }

    suspend fun perform(request: DbRequest) = withContext(Dispatchers.IO) {
        if (!request.needsWrite) {
            request.process(realm)
            return@withContext
        }


        realm.write {
            request.process(realm)
        }
    }

    suspend fun <T> perform(request: DbResponseRequest<T>): T {
        if (!request.needsWrite) {
            return request.process(realm)
        }

        return realm.write {
            return@write request.process(realm)
        }

    }

    suspend fun perform(requests: List<DbRequest>) = withContext(Dispatchers.IO) {

        realm.write {
            for (request in requests) {
                if (!request.needsWrite) {
                    continue
                }

                request.process(realm)
            }
        }
    }


}
