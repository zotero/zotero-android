package org.zotero.android.database
import io.realm.Realm
import io.realm.RealmConfiguration
import timber.log.Timber

class RealmDbStorage(val config: RealmConfiguration) {

    fun perform(coordinatorAction: (RealmDbCoordinator) -> Unit) {
        val coordinator = RealmDbCoordinator().init(config)
        coordinatorAction(coordinator)
    }

    inline fun <reified T : Any> perform(request: DbResponseRequest<T>): T {
        return perform(request = request, invalidateRealm = false, refreshRealm = false)
    }

    inline fun <reified T : Any> perform(
        request: DbResponseRequest<T>,
        refreshRealm: Boolean
    ): T {
        return perform(
            request = request,
            invalidateRealm = false,
            refreshRealm = refreshRealm
        )
    }

    inline fun <reified T : Any> perform(
        request: DbResponseRequest<T>,
        invalidateRealm: Boolean,
        functionDifferentiator: Unit = Unit
    ): T {
        return perform(
            request = request,
            refreshRealm = false,
            invalidateRealm = invalidateRealm
        )
    }

    inline fun <reified T: Any> perform(
        request: DbResponseRequest<T>,
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

    fun perform(request: DbRequest) {
        val coordinator = RealmDbCoordinator().init(config)
        coordinator.perform(request = request)
        coordinator.invalidate()
    }

    fun perform(requests: List<DbRequest>) {
        val coordinator = RealmDbCoordinator().init(config)
        coordinator.perform(requests)
        coordinator.invalidate()
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
        if (realm.isInTransaction) {
            Timber.e("RealmDbCoordinator: realm already in transaction $request")
            request.process(realm)
            return
        }

        realm.executeTransaction {
            request.process(realm)
        }
    }

    inline fun <reified T: Any> perform(request: DbResponseRequest<T>): T {
         if (!request.needsWrite) {
             return request.process(realm)
         }

         if (realm.isInTransaction) {
             Timber.e("RealmDbCoordinator: realm already in transaction $request")
             return request.process(realm)
         }
         var result: T? = null
         realm.executeTransaction {
             result = request.process(realm)
         }
         return result!!
    }

    fun perform(requests: List<DbRequest>) {
        if (realm.isInTransaction) {
            Timber.e("RealmDbCoordinator: realm already writing")
            for (request in requests) {
                if (!request.needsWrite) {
                    continue
                }
                Timber.e("type of: $request")
                request.process(realm)
            }
            return
        }

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
        realm.close()
    }
}
