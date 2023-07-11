package org.zotero.android.sync

import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmModel
import io.realm.RealmResults
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.core.EventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.Database
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.RealmDbCoordinator
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.RPageIndex
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.objects.Syncable
import org.zotero.android.database.requests.ReadUserChangedObjectsDbRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectUserChangeEventStream @Inject constructor(applicationScope: ApplicationScope) :
    EventStream<List<LibraryIdentifier>>(applicationScope)

class ObjectUserChangeObserver(
    val dbWrapper: DbWrapper,
    val observable: ObjectUserChangeEventStream,
    val applicationScope: ApplicationScope,
    val dispatchers: Dispatchers
) {

    private lateinit var pagesToken: RealmResults<RPageIndex>
    private lateinit var searchesToken: RealmResults<RSearch>
    private lateinit var itemsToken: RealmResults<RItem>
    private lateinit var collectionsToken: RealmResults<RCollection>

    init {
        applicationScope.launch {
            withContext(dispatchers.main) {
                setupObserving()
            }
        }
    }

    private fun setupObserving() {
        try {
            this.dbWrapper.realmDbStorage.perform(coordinatorAction = { coordinator ->
                this@ObjectUserChangeObserver.collectionsToken =
                    registerObserver(coordinator = coordinator)
                this@ObjectUserChangeObserver.itemsToken =
                    registerObserver(coordinator = coordinator)
                this@ObjectUserChangeObserver.searchesToken =
                    registerObserver(coordinator = coordinator)
                this@ObjectUserChangeObserver.pagesToken =
                    registerSettingsObserver(coordinator = coordinator)
            })
            } catch (error: Exception) {
                Timber.e(error, "RealmObjectChangeObserver: can't load objects to observe")
            }
    }

    private fun registerSettingsObserver(coordinator: RealmDbCoordinator): RealmResults<RPageIndex> {
        val objects = coordinator.perform(request = ReadUserChangedObjectsDbRequest(clazz = RPageIndex::class))

        objects.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RPageIndex>> { _, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    if (changeSet.insertions.isEmpty() && changeSet.changes.isEmpty()) {
                        return@OrderedRealmCollectionChangeListener
                    }
                    this.observable.emitAsync(listOf(LibraryIdentifier.custom(RCustomLibraryType.myLibrary)))
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(
                        changeSet.error,
                        "RealmObjectChangeObserver: RPageIndex observing error)"
                    )
                }

                else -> {
                    //no-op
                }
            }
        })
        return objects
    }

    private inline fun <reified T: RealmModel> registerObserver(coordinator: RealmDbCoordinator): RealmResults<T> {
        val objects = coordinator.perform(request = ReadUserChangedObjectsDbRequest(clazz = T::class))

        objects.addChangeListener { results, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    val correctedModifications = Database.correctedModifications(
                        changeSet.changes,
                        insertions = changeSet.insertions, deletions = changeSet.deletions
                    )
                    val updated =
                        (changeSet.insertions + correctedModifications).map { results[it] }
                    reportChangedLibraries(updated)
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(
                        changeSet.error,
                        "RealmObjectChangeObserver: ${T::class} observing error)"
                    )
                }
                else -> {
                    //no-op
                }
            }
        }

        return objects
    }

    private inline fun <reified T: RealmModel> reportChangedLibraries(objects: List<T?>) {
        val libraryIds = objects.mapNotNull {
            (it as? Syncable)?.libraryId
        }.toSet().toList()
        if (libraryIds.isEmpty()) {
            return
        }
        this.observable.emitAsync(libraryIds)
    }

}