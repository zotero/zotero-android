package org.zotero.android.screens.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmModel
import io.realm.RealmResults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.ARG_COLLECTIONS_SCREEN
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.Attachment.FileLinkType
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.EmptyTrashDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.database.requests.ReadAllAttachmentsFromCollectionDbRequest
import org.zotero.android.database.requests.ReadCollectionDbRequest
import org.zotero.android.database.requests.ReadCollectionsDbRequest
import org.zotero.android.database.requests.ReadItemsDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.database.requests.SetCollectionCollapsedDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.collectionedit.data.CollectionEditArgs
import org.zotero.android.screens.collections.controller.CollectionTreeController
import org.zotero.android.screens.collections.controller.CollectionTreeControllerInterface
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.collections.data.CollectionsError
import org.zotero.android.screens.dashboard.data.ShowDashboardLongPressBottomSheet
import org.zotero.android.screens.filter.data.FilterArgs
import org.zotero.android.screens.filter.data.UpdateFiltersEvent
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.AttachmentFileCleanupController
import org.zotero.android.sync.AttachmentFileCleanupController.DeletionType
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.CollectionIdentifier.CustomType
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import timber.log.Timber
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlin.reflect.KClass

@HiltViewModel
internal class CollectionsViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val fileCleanupController: AttachmentFileCleanupController,
    private val attachmentDownloader: AttachmentDownloader,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val collectionTreeController: CollectionTreeController,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<CollectionsViewState, CollectionsViewEffect>(CollectionsViewState()), CollectionTreeControllerInterface {

    private var allItems: RealmResults<RItem>? = null
    private var unfiledItems: RealmResults<RItem>? = null
    private var trashItems: RealmResults<RItem>? = null
    private var collections: RealmResults<RCollection>? = null

    private var isTablet: Boolean = false
    private var hasAlreadyShownList: Boolean = false

    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)
    private var library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    )
    private var itemsFilter: List<ItemsFilter> = emptyList()

    val screenArgs: CollectionsArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_COLLECTIONS_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded, StandardCharsets.UTF_8)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LongPressOptionItem) {
        onLongPressOptionsItemSelected(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(updateFiltersEvent: UpdateFiltersEvent) {
        this.itemsFilter = updateFiltersEvent.itemsFilter
    }

    fun init(isTablet: Boolean) = initOnce {
        EventBus.getDefault().register(this)
        this.isTablet = isTablet
        initViewState(screenArgs)
        collectionTreeController.init(
            libraryId = this.libraryId,
            isTablet = isTablet,
            includeItemCounts = defaults.showCollectionItemCounts(),
            collectionTreeControllerInterface = this
        )
        collectionTreeController.selectedCollectionId = viewState.selectedCollectionId
        viewModelScope.launch {
            loadData()
        }
    }

    private fun maybeRecreateItemsScreen() {
        if (screenArgs.shouldRecreateItemsScreen) {
            val collectionToTap =
                viewState.fixedCollections.values.firstOrNull { it.identifier == viewState.selectedCollectionId }
                    ?: collectionTreeController.getCollectionByCollectionId(viewState.selectedCollectionId)
            if (collectionToTap != null) {
                onItemTapped(collectionToTap)
            }
        }
    }

    private fun initViewState(args: CollectionsArgs) {
        this.libraryId = args.libraryId
        this.library = Library(
            identifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
            name = "",
            metadataEditable = false,
            filesEditable = false
        )

        updateState {
            copy(
                fixedCollections = persistentMapOf(
                    CustomType.all to Collection.initWithCustomType(
                        type = CustomType.all,
                        itemCount = 0
                    ), CustomType.unfiled to Collection.initWithCustomType(
                        type = CustomType.unfiled,
                        itemCount = 0
                    ),
                    CustomType.trash to Collection.initWithCustomType(
                        type = CustomType.trash,
                        itemCount = 0
                    )
                ),
                selectedCollectionId = args.selectedCollectionId,
                showCollectionItemCounts = defaults.showCollectionItemCounts()
            )
        }

        if (isTablet) {
            updateState {
                copy(tabletFilterArgs = createShowFilterArgs())
            }
        }

    }

    private suspend fun loadData() {
        this@CollectionsViewModel.library =
            perform(
                dbWrapper = dbWrapperMain,
                invalidateRealm = false,
                request = ReadLibraryDbRequest(libraryId = libraryId)
            ).ifFailure {
                Timber.e(it, "CollectionsActionHandler: can't change collapsed")
                return
            }

        initRequestAndStartObservingCollectionResults()

        if (viewState.showCollectionItemCounts) {
            maybeInitRequestAndStartObservingAllItemsCount()
            maybeInitRequestAndStartObservingUnfiledItemsCount()
            maybeInitRequestAndStartObservingTrashItemsCount()
        }
    }

    private fun initRequestAndStartObservingCollectionResults() {
        collections = dbWrapperMain.realmDbStorage.perform(
            ReadCollectionsDbRequest(
                libraryId = libraryId,
                isAsync = true
            )
        )
        collections?.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RCollection>> { objects, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    collectionTreeController.reactToCollectionsDbUpdate(
                        collections = objects,
                        changeSet = changeSet,
                    )
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    collectionTreeController.reactToCollectionsDbUpdate(
                        collections = objects,
                        changeSet = changeSet,
                    )
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(
                        changeSet.error,
                        "CollectionsViewModel: could not load results"
                    )
                }
            }
        })
    }

    private fun maybeInitRequestAndStartObservingAllItemsCount() {
        allItems = dbWrapperMain.realmDbStorage.perform(
            request = ReadItemsDbRequest(
                collectionId = CollectionIdentifier.custom(
                    CustomType.all
                ),
                libraryId = libraryId,
                defaults = defaults,
                isAsync = true,
            )
        )
        observeItemCount(
            results = allItems,
            customType = CustomType.all
        )
    }
    private fun maybeInitRequestAndStartObservingUnfiledItemsCount() {
        unfiledItems = dbWrapperMain.realmDbStorage.perform(
            request = ReadItemsDbRequest(
                collectionId = CollectionIdentifier.custom(
                    CustomType.unfiled
                ),
                libraryId = libraryId,
                defaults = defaults,
                isAsync = true,
            )
        )
        observeItemCount(
            results = unfiledItems,
            customType = CustomType.unfiled
        )
    }

    private fun maybeInitRequestAndStartObservingTrashItemsCount() {
        trashItems = dbWrapperMain.realmDbStorage.perform(
            request = ReadItemsDbRequest(
                collectionId = CollectionIdentifier.custom(
                    CustomType.trash
                ),
                libraryId = libraryId,
                defaults = defaults,
                isAsync = true,
            )
        )

        observeItemCount(
            results = trashItems,
            customType = CustomType.trash
        )
    }

    private fun observeItemCount(
        results: RealmResults<RItem>?,
        customType: CustomType
    ) {
        results?.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { items, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    reactToItemsCountDbUpdate(itemsCount = items.size, customType = customType)
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    reactToItemsCountDbUpdate(itemsCount = items.size, customType = customType)
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "CollectionsViewModel: could not load results")
                }

                else -> {
                    //no-op
                }
            }
        })
    }

    private fun reactToItemsCountDbUpdate(itemsCount: Int, customType: CustomType) {
        val fixedCollection = Collection.initWithCustomType(
            type = customType,
            itemCount = itemsCount
        )
        updateState {
            copy(fixedCollections = (viewState.fixedCollections + (customType to fixedCollection)).toPersistentMap())
        }
    }

    override fun onItemTapped(collection: Collection) {
        viewModelScope.launch {
            updateState {
                copy(selectedCollectionId = collection.identifier)
            }
            collectionTreeController.selectedCollectionId = viewState.selectedCollectionId
            fileStore.setSelectedCollectionIdAsync(collection.identifier)
            ScreenArguments.allItemsArgs = AllItemsArgs(
                collection = collection,
                library = this@CollectionsViewModel.library,
                searchTerm = null,
                error = null
            )
            val collectionsArgs = CollectionsArgs(
                libraryId = this@CollectionsViewModel.libraryId,
                selectedCollectionId = collection.identifier,
                shouldRecreateItemsScreen = this@CollectionsViewModel.isTablet
            )
            val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64Async(collectionsArgs, StandardCharsets.UTF_8)
            triggerEffect(CollectionsViewEffect.NavigateToAllItemsScreen(encodedArgs))
        }
    }

    fun onItemChevronTapped(collection: Collection) {
        val libraryId = this.libraryId
        val collapsed = viewState.collapsed[collection.identifier] ?: return
        collectionTreeController.setCollapsed(
            collapsed = !collapsed, collection.identifier
        )

        val request = SetCollectionCollapsedDbRequest(
            collapsed = !collapsed,
            identifier = collection.identifier,
            libraryId = libraryId
        )
        viewModelScope.launch {
            perform(dbWrapperMain, request)
                .ifFailure {
                    Timber.e(it, "CollectionsActionHandler: can't change collapsed")
                    return@launch
                }
        }
    }

    override fun onCleared() {
        collectionTreeController.cancel()
        EventBus.getDefault().unregister(this)
        allItems?.removeAllChangeListeners()
        unfiledItems?.removeAllChangeListeners()
        trashItems?.removeAllChangeListeners()
        collections?.removeAllChangeListeners()
        super.onCleared()
    }

    fun onAdd() {
        ScreenArguments.collectionEditArgs = CollectionEditArgs(
            library = this.library,
            key = null,
            name = "",
            parent = null,
        )
        triggerEffect(CollectionsViewEffect.ShowCollectionEditEffect)
    }

    fun onItemLongTapped(collection: Collection) {
        val actions = mutableListOf<LongPressOptionItem>()

        when (collection.identifier) {
            is CollectionIdentifier.collection -> {
                if (this.library.metadataEditable) {
                    actions.add(LongPressOptionItem.CollectionEdit(collection))
                    actions.add(LongPressOptionItem.CollectionNewSubCollection(collection))
                    actions.add(LongPressOptionItem.CollectionDelete(collection))

                    if (collection.itemCount > 0) {
                        actions.addAll(
                            0,
                            listOf(
                                LongPressOptionItem.CollectionDownloadAttachments(collection.identifier),
                                LongPressOptionItem.CollectionRemoveDownloads(collection.identifier)
                            )
                        )
                    }
                }
            }

            is CollectionIdentifier.custom -> run {
                if (collection.itemCount <= 0) {
                    return@run
                }
                actions.add(LongPressOptionItem.CollectionRemoveDownloads(collection.identifier))
                val type = collection.identifier.type
                when(type) {
                    CustomType.trash -> {
                        if (this.library.metadataEditable) {
                            actions.add(LongPressOptionItem.CollectionEmptyTrash)
                        }
                    }

                    CustomType.publications, CustomType.all, CustomType.unfiled -> {
                        actions.add(0, LongPressOptionItem.CollectionDownloadAttachments(collection.identifier))
                    }
                }
            }

            is CollectionIdentifier.search -> {
                //no-op
            }
        }
        if (actions.isEmpty()) {
            return
        }
        EventBus.getDefault().post(
            ShowDashboardLongPressBottomSheet(
                title = collection.name,
                longPressOptionItems = actions
            )
        )
    }

    private fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        viewModelScope.launch {
            when (longPressOptionItem) {
                is LongPressOptionItem.CollectionEdit -> {
                    onEdit(longPressOptionItem.collection)
                }

                is LongPressOptionItem.CollectionRemoveDownloads -> {
                    removeDownloads(longPressOptionItem.collectionId)
                }

                is LongPressOptionItem.CollectionNewSubCollection -> {
                    onAddSubcollection(longPressOptionItem.collection)
                }

                is LongPressOptionItem.CollectionDelete -> {
                    deleteCollection(
                        clazz = RCollection::class,
                        listOf(longPressOptionItem.collection.identifier.keyGet!!)
                    )
                }

                is LongPressOptionItem.CollectionEmptyTrash -> {
                    emptyTrash()
                }

                is LongPressOptionItem.CollectionDownloadAttachments -> {
                    downloadAttachments(longPressOptionItem.collectionId)
                }

                else -> {
                    //no-op
                }
            }
        }
    }

    private fun downloadAttachments(collectionId: CollectionIdentifier) {
        try {
            val items = dbWrapperMain.realmDbStorage.perform(
                request = ReadAllAttachmentsFromCollectionDbRequest(
                    collectionId = collectionId,
                    libraryId = this.library.identifier,
                    defaults = this.defaults
                ),
            )
            val attachments = items.mapNotNull { item ->
                val attachment = AttachmentCreator.attachment(
                    item,
                    fileStorage = this.fileStore,
                    defaults = this.defaults,
                    urlDetector = null,
                    isForceRemote = false
                ) ?: return@mapNotNull null

                when (attachment.type) {
                    is Attachment.Kind.file -> {
                        val linkType = attachment.type.linkType
                        when (linkType) {
                            FileLinkType.importedFile, FileLinkType.importedUrl -> {
                                return@mapNotNull attachment to item.parent?.key
                            }
                            else -> {
                                //no-op
                            }

                        }
                    }

                    else -> {
                        //no-op
                    }
                }
                return@mapNotNull null
            }
            this.attachmentDownloader.batchDownload(
                attachments = attachments
            )
        } catch (error: Exception) {
            Timber.e(error, "CollectionsViewModel: download attachments")
        }

    }

    private fun emptyTrash() {
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = EmptyTrashDbRequest(libraryId = this@CollectionsViewModel.library.identifier)
            ).ifFailure {
                Timber.e(it, "CollectionsActionHandler: can't empty trash")
                return@launch
            }
        }
    }

    private fun removeDownloads(collectionId: CollectionIdentifier) {
        try {
            val items = dbWrapperMain.realmDbStorage.perform(
                request = ReadItemsDbRequest(
                    collectionId = collectionId,
                    libraryId = this.libraryId,
                    defaults = this.defaults,
                    isAsync = false
                )
            )
            val keys = items.map { it.key }.toSet()
            fileCleanupController.delete(
                type = DeletionType.allForItems(keys, this.libraryId, collectionId),
                completed = null,
            )
        } catch (exception: Exception) {
            Timber.e(exception, "CollectionsViewModel: remove downloads")
        }
    }

    private suspend fun deleteCollection(clazz: KClass<out RealmModel>, keys: List<String>) {
        val request = MarkObjectsAsDeletedDbRequest(
            clazz = clazz,
            keys = keys,
            libraryId = this.library.identifier
        )
        perform(
            dbWrapper = dbWrapperMain,
            request = request
        ).ifFailure {
            Timber.e(it, "CollectionsActionHandler: can't delete object")
            updateState {
                copy(error = CollectionsError.deletion)
            }
            return
        }
    }

    private fun onEdit(collection: Collection) {
        val parentKey = collectionTreeController.parentNode(collection.identifier)?.collection?.identifier?.keyGet
        val parent: Collection?
        if (parentKey != null) {
            val request =
                ReadCollectionDbRequest(libraryId = this.library.identifier, key = parentKey)
            val rCollection = dbWrapperMain.realmDbStorage.perform(request = request)
            parent = Collection.initWithCollection(objectS = rCollection, itemCount = 0)
        } else {
            parent = null
        }
        ScreenArguments.collectionEditArgs = CollectionEditArgs(
            library = this.library,
            key = collection.identifier.keyGet,
            name = collection.name,
            parent = parent,
        )
        triggerEffect(CollectionsViewEffect.ShowCollectionEditEffect)
    }

    private fun onAddSubcollection(collection: Collection) {
        ScreenArguments.collectionEditArgs = CollectionEditArgs(
            library = this.library,
            key = null,
            name = "",
            parent = collection,
        )
        triggerEffect(CollectionsViewEffect.ShowCollectionEditEffect)
    }

    fun navigateToLibraries() {
        viewModelScope.launch {
            val collectionsArgs = CollectionsArgs(
                libraryId = fileStore.getSelectedLibraryAsync(),
                fileStore.getSelectedCollectionIdAsync()
            )
            val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(collectionsArgs, StandardCharsets.UTF_8)
            triggerEffect(CollectionsViewEffect.NavigateToLibrariesScreen(encodedArgs))
        }
    }

    private fun createShowFilterArgs(): FilterArgs {
        val selectedTags =
            itemsFilter.filterIsInstance<ItemsFilter.tags>().flatMap { it.tags }.toSet()
        val filterArgs = FilterArgs(
            filters = itemsFilter,
            collectionId = viewState.selectedCollectionId,
            libraryId = this.library.identifier,
            selectedTags = selectedTags
        )
        return filterArgs
    }

    override fun sendChangesToUi(
        updatedItemsWithChildren: PersistentList<CollectionItemWithChildren>?,
        updatedCollapsed: PersistentMap<CollectionIdentifier, Boolean>?
    ) {
        viewModelScope.launch {
            updateState {
                copy(
                    collectionItemsToDisplay = updatedItemsWithChildren
                        ?: viewState.collectionItemsToDisplay,
                    collapsed = updatedCollapsed
                        ?: viewState.collapsed,
                )
            }
            if (!hasAlreadyShownList) {
                hasAlreadyShownList = true
                updateState {
                    copy(
                        libraryName = this@CollectionsViewModel.library.name,
                        lce = LCE2.Content
                    )
                }
                maybeRecreateItemsScreen()
            }
        }
    }

}

internal data class CollectionsViewState(
    val libraryName: String = "",
    val collectionItemsToDisplay: ImmutableList<CollectionItemWithChildren> = persistentListOf(),
    val collapsed: PersistentMap<CollectionIdentifier, Boolean> = persistentMapOf(),
    val selectedCollectionId: CollectionIdentifier = CollectionIdentifier.custom(
        CustomType.all
    ),
    val editingData: Triple<String?, String, Collection?>? = null,
    val error: CollectionsError? = null,
    val lce: LCE2 = LCE2.Loading,
    val tabletFilterArgs: FilterArgs? = null,
    val showCollectionItemCounts: Boolean = false,

    val fixedCollections: PersistentMap<CustomType, Collection> = persistentMapOf(),
    ) : ViewState {
    fun isCollapsed(snapshot: CollectionItemWithChildren): Boolean {
       return collapsed[snapshot.collection.identifier] != false
    }
}

internal sealed class CollectionsViewEffect : ViewEffect {
    object NavigateBack : CollectionsViewEffect()
    data class NavigateToAllItemsScreen(val screenArgs: String) : CollectionsViewEffect()
    data class NavigateToLibrariesScreen(val screenArgs: String) : CollectionsViewEffect()
    object ShowCollectionEditEffect : CollectionsViewEffect()
}
