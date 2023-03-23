package org.zotero.android.screens.collections

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.ReadCollectionsDbRequest
import org.zotero.android.database.requests.ReadItemsDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetMimeTypeUseCase
import org.zotero.android.helpers.SelectMediaUseCase
import org.zotero.android.screens.collections.data.CollectionTree
import org.zotero.android.screens.collections.data.CollectionTreeBuilder
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.collections.data.CollectionsError
import org.zotero.android.sync.AttachmentFileCleanupController
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CollectionsViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val selectMedia: SelectMediaUseCase,
    private val fileDownloader: AttachmentDownloader,
    private val getMimeTypeUseCase: GetMimeTypeUseCase,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    private val schemaController: SchemaController,
    private val dispatchers: Dispatchers,
    private val fileCleanupController: AttachmentFileCleanupController,
) : BaseViewModel2<CollectionsViewState, CollectionsViewEffect>(CollectionsViewState()) {


    fun init() = initOnce {
        viewModelScope.launch {
            val args = ScreenArguments.collectionsArgs
            initViewState(args)
            loadData()
        }

    }

    private fun initViewState(args: CollectionsArgs) {
        updateState {
            copy(
                libraryId = args.libraryId,
                library = Library(
                    identifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
                    name = "",
                    metadataEditable = false,
                    filesEditable = false
                ),
                selectedCollectionId = args.selectedCollectionId,
                collectionTree = CollectionTree(
                    nodes = mutableListOf(),
                    collections = mutableMapOf(),
                    collapsed = mutableMapOf()
                )
            )
        }

    }

    private fun loadData() {
        val libraryId = viewState.libraryId
        val includeItemCounts = defaults.showCollectionItemCounts()

        try {
            dbWrapper.realmDbStorage.perform { coordinator ->
                val library =
                    coordinator.perform(request = ReadLibraryDbRequest(libraryId = libraryId))
                val collections =
                    coordinator.perform(request = ReadCollectionsDbRequest(libraryId = libraryId))

                var allItemCount = 0
                var unfiledItemCount = 0
                var trashItemCount = 0

                if (includeItemCounts) {
                    val allItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.all
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    allItemCount = allItems.size

                    val unfiledItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.unfiled
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    unfiledItemCount = unfiledItems.size

                    val trashItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.trash
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    trashItemCount = trashItems.size
                    observeItemCount(
                        results = allItems,
                        customType = CollectionIdentifier.CustomType.all
                    )
                    observeItemCount(
                        results = unfiledItems,
                        customType = CollectionIdentifier.CustomType.unfiled
                    )
                    observeItemCount(
                        results = trashItems,
                        customType = CollectionIdentifier.CustomType.trash
                    )
                }

                val collectionTree = CollectionTreeBuilder.collections(
                    collections,
                    libraryId = libraryId,
                    includeItemCounts = includeItemCounts
                )
                collectionTree.insert(
                    collection = Collection.initWithCustomType(
                        CollectionIdentifier.CustomType.all,
                        itemCount = allItemCount
                    ), index = 0
                )
                collectionTree.append(
                    collection = Collection.initWithCustomType(
                        CollectionIdentifier.CustomType.unfiled,
                        itemCount = unfiledItemCount
                    )
                )
                collectionTree.append(
                    collection = Collection.initWithCustomType(
                        CollectionIdentifier.CustomType.trash,
                        itemCount = trashItemCount
                    )
                )

                collections.removeAllChangeListeners()
                collections.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RCollection>> { objects, changeSet ->
                    val state = changeSet.state
                    when (state) {
                        OrderedCollectionChangeSet.State.INITIAL -> {
                            //no-op
                        }
                        OrderedCollectionChangeSet.State.UPDATE -> {
                            update(collections = objects, includeItemCounts = includeItemCounts)
                        }
                        OrderedCollectionChangeSet.State.ERROR -> {
                            Timber.e(
                                changeSet.error,
                                "CollectionsViewModel: could not load results"
                            )
                        }
                    }
                })
                updateState {
                    copy(
                        collectionTree = collectionTree,
                        library = library
                    )
                }
            }
        } catch (error: Exception) {
            Timber.e(error, "CollectionsActionHandlers: can't load data")
            updateState {
                copy(error = CollectionsError.dataLoading)
            }
        }
    }

    private fun observeItemCount(results: RealmResults<RItem>, customType: CollectionIdentifier.CustomType) {
        results.removeAllChangeListeners()
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    update(itemsCount = items.size, customType = customType)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "CollectionsViewModel: could not load results")
                }
            }
        })
    }

    private fun update(itemsCount: Int, customType: CollectionIdentifier.CustomType) {
        val collectionTree = viewState.collectionTree
        collectionTree.update(
            collection = Collection.initWithCustomType(
                type = customType,
                itemCount = itemsCount
            )
        )
        updateState {
            copy(collectionTree = collectionTree)
        }
    }

    private fun update(collections: RealmResults<RCollection>, includeItemCounts: Boolean) {
        val tree = CollectionTreeBuilder.collections(collections, libraryId = viewState.libraryId, includeItemCounts = includeItemCounts)
        val collectionTree = viewState.collectionTree
        collectionTree.replace(matching = { it.isCollection }, tree = tree)

        if (viewState.collectionTree.collection(viewState.selectedCollectionId) == null) {
            updateState {
                copy(selectedCollectionId = CollectionIdentifier.custom(CollectionIdentifier.CustomType.all))
            }
        }
    }

}

internal data class  CollectionsViewState(
    val libraryId: LibraryIdentifier = LibraryIdentifier.group(0),
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val collectionTree: CollectionTree = CollectionTree(
        mutableListOf(),
        mutableMapOf(),
        mutableMapOf()
    ),
    val selectedCollectionId: CollectionIdentifier = CollectionIdentifier.custom(CollectionIdentifier.CustomType.all),
    val editingData: Triple<String?, String, Collection?>? = null,
    val error: CollectionsError? = null,
    val lce: LCE2 = LCE2.Loading,
) : ViewState

internal sealed class  CollectionsViewEffect : ViewEffect {
    object NavigateBack : CollectionsViewEffect()
}
