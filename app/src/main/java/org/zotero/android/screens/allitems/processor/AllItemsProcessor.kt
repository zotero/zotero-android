package org.zotero.android.screens.allitems.processor

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.attachmentdownloader.AttachmentDownloader
import org.zotero.android.attachmentdownloader.AttachmentDownloaderEventStream
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.ReadItemsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.ItemAccessory
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.allitems.data.ItemsState
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.screens.sortpicker.data.SortDirectionResult
import org.zotero.android.sync.AttachmentCreator
import org.zotero.android.sync.AttachmentFileCleanupController
import org.zotero.android.sync.AttachmentFileDeletedNotification
import org.zotero.android.sync.AttachmentFileShareController
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.UrlDetector
import org.zotero.android.uicomponents.attachmentprogress.State
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import timber.log.Timber
import javax.inject.Inject

class AllItemsProcessor @Inject constructor(
    dispatchers: Dispatchers,
    private val defaults: Defaults,
    private val fileStore: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val attachmentDownloaderEventStream: AttachmentDownloaderEventStream,
    private val fileDownloader: AttachmentDownloader,
    private val fileShareController: AttachmentFileShareController,
    private val fileCleanupController: AttachmentFileCleanupController,
) {
    private lateinit var processorInterface: AllItemsProcessorInterface

    private var listenersCoroutineScope: CoroutineScope = CoroutineScope(dispatchers.io)

    private val limitedParallelismDispatcher =
        kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1)
    private var resultsProcessorCoroutineScope: CoroutineScope? = null

    private var results: RealmResults<RItem>? = null

    private val itemAccessories = mutableMapOf<String, ItemAccessory> ()
    private var updateItemKey: String? = null
    private var downloadBatchData: ItemsState.DownloadBatchData? = null
    private var attachmentToOpen: String? = null
    private var sortType: ItemsSortType = ItemsSortType.default
    private var mutableItemCellModels: MutableList<ItemCellModel> = mutableListOf()
    private var mutableDownloadingAcessories: MutableMap<String, ItemCellModel.Accessory?> = mutableMapOf()

    private val library: Library get() {
        return processorInterface.currentLibrary()
    }
    private val collection: Collection get() {
        return processorInterface.currentCollection()
    }

    private val onSearchStateFlow = MutableStateFlow("")

    private val quotationExpression = "(\"[^\"]+\"?)"

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(sortDirectionResult: SortDirectionResult) {
        setSortOrder(sortDirectionResult.isAscending)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItemsShowItem) {
            val collectionKey: String?
            val identifier = this.collection.identifier
            when (identifier) {
                is CollectionIdentifier.collection ->
                    collectionKey = identifier.key
                is CollectionIdentifier.search, is CollectionIdentifier.custom ->
                    collectionKey = null
            }

            val type = singlePickerResult.id
            val creation = DetailType.creation(
                type = type,
                child = null,
                collectionKey = collectionKey
            )
            processorInterface.showItemDetailWithDelay(creation)
        } else if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.AllItemsSortPicker) {
            onSortFieldChanged(singlePickerResult.id)
        }
    }

    private val onAttachmentFileDeletedStateFlow = MutableStateFlow<EventBusConstants.AttachmentFileDeleted?>(null)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(attachmentFileDeleted: EventBusConstants.AttachmentFileDeleted) {
        onAttachmentFileDeletedStateFlow.tryEmit(attachmentFileDeleted)
    }

    private fun setupOnAttachmentFileDeletedStateFlow() {
        onAttachmentFileDeletedStateFlow
            .debounce(100)
            .map {
                if (it != null) {
                    updateDeletedAttachmentFiles(it.notification)
                }
            }
            .launchIn(viewModelScope)
    }

    lateinit var viewModelScope:CoroutineScope
    fun init(viewModelScope: CoroutineScope, allItemsProcessorInterface: AllItemsProcessorInterface, searchTerm: String?) {
        this.viewModelScope = viewModelScope
        this.processorInterface = allItemsProcessorInterface
        EventBus.getDefault().register(this)
        setupFlowListeners(viewModelScope)
        this.sortType = this.defaults.getItemsSortType()

        val results = results(
            searchText = searchTerm,
            filters = emptyList(),
            sortType = this.sortType,
        )
        processResultsReset(results)
    }

    private fun setupFlowListeners(viewModelScope: CoroutineScope) {
        setupSearchStateFlow(viewModelScope)
        setupOnAttachmentFileDeletedStateFlow()
        setupFileObservers()
    }

    private fun setupSearchStateFlow(viewModelScope: CoroutineScope) {
        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                search(
                    text = if (text.isEmpty()) null else text,
                    filters = processorInterface.currentFilters()
                )
            }
            .launchIn(viewModelScope)
    }

    private fun setupFileObservers() {
        attachmentDownloaderEventStream.flow()
            .onEach { update ->
                val (progress, remainingCount, totalCount) = fileDownloader.batchData
                val batchData = progress?.let { ItemsState.DownloadBatchData.init(progress = progress, remaining = remainingCount, total = totalCount)}
                process(update = update, batchData = batchData)

                if (update.kind is AttachmentDownloader.Update.Kind.progress) {
                    return@onEach
                }
                if (this.attachmentToOpen != update.key) {
                    return@onEach
                }
                attachmentOpened(update.key)
                when (update.kind) {
                    AttachmentDownloader.Update.Kind.ready -> {
                        showAttachment(key = update.key, parentKey = update.parentKey)
                    }
                    is AttachmentDownloader.Update.Kind.failed -> {
                        //TODO implement when unzipping is supported
                    }
                    else -> {}
                }
            }
            .launchIn(listenersCoroutineScope)
    }

    private fun attachmentOpened(key: String) {
        if (this.attachmentToOpen != key) {
            return
        }
        this.attachmentToOpen = null
    }

    private fun onSortFieldChanged(id: String) {
        val field = ItemsSortType.Field.values().first { it.titleStr == id }
        val sortType = this.sortType.copy(
            field = field,
            ascending = field.defaultOrderAscending
        )
        changeSortType(
            sortType = sortType,
            filters = processorInterface.currentFilters(),
            searchTerm = processorInterface.currentSearchTerm()
        )
    }

    private fun search(
        text: String?,
        filters: List<ItemsFilter>,
    ) {
        val results = results(
            searchText = text,
            filters = filters,
            sortType = this.sortType,
        )
        processResultsReset(results)
    }

    internal fun results(
        searchText: String?,
        filters: List<ItemsFilter>,
        sortType: ItemsSortType,
    ): RealmResults<RItem> {
        var searchComponents = listOf<String>()
        val text = searchText
        if (!text.isNullOrEmpty()) {
            searchComponents = createComponents(text)
        }
        val request = ReadItemsDbRequest(
            collectionId = this.collection.identifier,
            libraryId = this.library.id,
            filters = filters,
            sortType = sortType,
            searchTextComponents = searchComponents,
            defaults = this.defaults,
            isAsync = true
        )
        return dbWrapperMain.realmDbStorage.perform(request = request)
    }

    private fun createComponents(searchTerm: String): List<String> {
        val normalizedSearchTerm = searchTerm
            .replace("“", "\"")
            .replace("”", "\"")

        val matches =
            Regex(quotationExpression).findAll(normalizedSearchTerm).map { it }.toMutableList()

        if (matches.isEmpty()) {
            return separateComponents(normalizedSearchTerm)
        }

        val components = mutableListOf<String>()
        for ((idx, match) in matches.withIndex()) {
            if (match.range.start > 0) {
                val lowerBound = if (idx == 0) 0 else matches[idx - 1].range.endInclusive
                val precedingComponents = separateComponents(
                    normalizedSearchTerm.substring(
                        lowerBound,
                        match.range.start
                    )
                )
                components.addAll(precedingComponents)
            }
            val upperBound =
                if (normalizedSearchTerm.substring(match.range.endInclusive - 1) == "\"") {
                    match.range.endInclusive - 1
                } else {
                    match.range.endInclusive
                }
            components.add(normalizedSearchTerm.substring(match.range.start + 1, upperBound))
        }

        val match = matches.lastOrNull()
        if (match != null && match.range.endInclusive != normalizedSearchTerm.length - 1) {
            val lastComponents = separateComponents(
                normalizedSearchTerm.substring(
                    match.range.endInclusive,
                    normalizedSearchTerm.length
                )
            )
            components.addAll(lastComponents)
        }

        return components
    }

    private fun separateComponents(string: String): List<String> {
        return string.split(" ").filter{ it.isNotEmpty() }
    }

    fun attachment(key: String, parentKey: String?): Pair<Attachment, Library>? {
        val accessory = itemAccessories[parentKey ?: key] ?: return null
        val attachment = accessory.attachmentGet ?: return null
        return attachment to this.library
    }

    private fun showAttachment(key: String, parentKey: String?) {
        val attachmentResult = attachment(key = key, parentKey = parentKey)
        if (attachmentResult == null) {
            return
        }
        val (attachment, library) = attachmentResult
        processorInterface.show(attachment = attachment, library = library)
    }

    private fun process(
        update: AttachmentDownloader.Update,
        batchData: ItemsState.DownloadBatchData?
    ) {
        val updateKey = update.parentKey ?: update.key
        val accessory = itemAccessories[updateKey] ?: return
        val attachment = accessory.attachmentGet ?: return
        when (update.kind) {
            AttachmentDownloader.Update.Kind.ready -> {
                val updatedAttachment =
                    attachment.changed(location = Attachment.FileLocation.local)
                        ?: return

                itemAccessories[updateKey] = ItemAccessory.attachment(updatedAttachment)

                this.updateItemKey = updateKey
                if (this.downloadBatchData != batchData) {
                    this.downloadBatchData = batchData
                }
            }

            AttachmentDownloader.Update.Kind.cancelled, is AttachmentDownloader.Update.Kind.failed, is AttachmentDownloader.Update.Kind.progress -> {
                this.updateItemKey = updateKey
                if (this.downloadBatchData != batchData) {
                    this.downloadBatchData = batchData
                }


            }
        }
        when (update.kind) {
            is AttachmentDownloader.Update.Kind.progress -> {
                mutableDownloadingAcessories[updateKey] = cellAccessory(itemAccessories[updateKey])
                sendChangedToUi(includeAccessories = true)
            }
            else -> {
                mutableDownloadingAcessories.remove(updateKey)
                updateCellAccessoryForItem(updateKey)
                sendChangedToUi(includeItemCellModels = true, includeAccessories = true)
            }
        }
    }



    private fun updateCellAccessoryForItem(updateKey: String) {
        val cellAccessory = cellAccessory(itemAccessories[updateKey])
        val index = mutableItemCellModels.indexOfFirst { it.key == updateKey }
        if (index != -1) {
            mutableItemCellModels[index] =
                mutableItemCellModels[index].updateAccessory(cellAccessory)
        }
    }

    private fun CoroutineScope.generateCellModels(
        item: RItem,
        itemCellModelsToUpdate: MutableList<ItemCellModel>,
        insertionIndex: Int? = null,
    ) {
        val accessory = itemAccessories[item.key]
        val typeName = ""
        val cellModel = ItemCellModel.init(
            item = item,
            accessory = cellAccessory(accessory),
            typeName = typeName
        )
        val indexToReplace = itemCellModelsToUpdate.indexOfFirst { it.key == cellModel.key }
        if (!isActive) {
            return
        }
        if (indexToReplace != -1) {
            itemCellModelsToUpdate[indexToReplace] = cellModel
        } else if (insertionIndex != null) {
            itemCellModelsToUpdate.add(insertionIndex, cellModel)
        } else {
            itemCellModelsToUpdate.add(cellModel)
        }
    }

    private fun startObservingResults() {
        this.results!!.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { items, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    reactToDbUpdate(changeSet, items)

                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    reactToDbUpdate(changeSet, items)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "ItemsViewController: could not load results")
                    processorInterface.showError(ItemsError.dataLoading)
                }
                else -> {
                    //no-op
                }
            }
        })
    }

    private fun reactToDbUpdate(
        changeSet: OrderedCollectionChangeSet,
        items: RealmResults<RItem>
    ) {
        val frozenItems = items.freeze()

        val deletions = changeSet.deletions
        var insertions = changeSet.insertions
        val modifications = changeSet.changes

        if (deletions.isEmpty() && insertions.isEmpty() && modifications.isEmpty()) {
            insertions = IntArray(frozenItems.size) { it }
        }

        processorInterface.updateLCE(LCE2.Content)

        processUpdate(
            frozenItems = frozenItems,
            deletions = deletions,
            insertions = insertions,
            modifications = modifications
        )

        processorInterface.updateTagFilter()
    }

    private fun processResultsReset(results: RealmResults<RItem>) {
        resultsProcessorCoroutineScope?.cancel()
        resultsProcessorCoroutineScope = CoroutineScope(limitedParallelismDispatcher)
        removeAllListenersFromResultsList()
        this.results = results

        mutableItemCellModels = mutableListOf()
        sendChangedToUi(includeItemCellModels = true)

        startObservingResults()
        processorInterface.updateTagFilter()
    }

    private fun removeAllListenersFromResultsList() {
        this.results?.removeAllChangeListeners()
    }

    private fun sendChangedToUi(includeItemCellModels: Boolean = false, includeAccessories: Boolean = false) {
        val updatedItemCellModels =
            if (includeItemCellModels) {
                val copyOfItemCellModels = mutableItemCellModels.toList()
                copyOfItemCellModels.toMutableStateList()
            } else {
                null
            }

        val updatedDownloadingAccessories = if (includeAccessories) {
            val copyOfDownloadingAccessories = mutableDownloadingAcessories.toMap()
           mutableStateMapOf<String, ItemCellModel.Accessory?>().apply {
               putAll(copyOfDownloadingAccessories)
           }
        } else {
            null
        }

        processorInterface.sendChangesToUi(
            updatedItemCellModels = updatedItemCellModels,
            updatedDownloadingAccessories = updatedDownloadingAccessories
        )
    }
    internal fun filter(
        searchTerm: String?,
        filters: List<ItemsFilter>,
    ) {
        val results = results(
            searchText = searchTerm,
            filters = filters,
            sortType = this.sortType,
        )
        processResultsReset(results)
    }

    private fun changeSortType(
        searchTerm: String?,
        filters: List<ItemsFilter>,
        sortType: ItemsSortType,
    ) {
        this.sortType = sortType
        defaults.setItemsSortType(sortType)

        val results = results(
            searchText = searchTerm,
            filters = filters,
            sortType = sortType,
        )
        processResultsReset(results)
    }

    private fun processUpdate(
        frozenItems: RealmResults<RItem>,
        deletions: IntArray,
        insertions: IntArray,
        modifications: IntArray
    ) {
        val updateThreshold = 20
        var currentProcessingCount = 0

        resultsProcessorCoroutineScope!!.launch {
            val itemCellModelsToUpdate = mutableItemCellModels
            deletions.sorted().reversed().forEach { idx ->
                if (!isActive) {
                    return@launch
                }
                val modelToRemove = itemCellModelsToUpdate.removeAt(idx)
                this@AllItemsProcessor.itemAccessories.remove(modelToRemove.key)
            }

            insertions.forEach { idx ->
                if (!isActive) {
                    return@launch
                }
                val item = frozenItems[idx]!!

                val itemAccessory = accessory(item)
                if (itemAccessory != null) {
                    this@AllItemsProcessor.itemAccessories.put(item.key, itemAccessory)
                }
                if (!isActive) {
                    return@launch
                }
                generateCellModels(
                    item = item,
                    itemCellModelsToUpdate = itemCellModelsToUpdate,
                    insertionIndex = idx
                )
                if (!isActive) {
                    return@launch
                }
                currentProcessingCount++
                if (currentProcessingCount % updateThreshold == 0) {
                    sendChangedToUi(includeItemCellModels = true)
                }
            }
            modifications.forEach { idx ->
                if (!isActive) {
                    return@launch
                }
                val item = frozenItems[idx]!!
                val itemAccessory = accessory(item)
                if (itemAccessory != null) {
                    this@AllItemsProcessor.itemAccessories.put(item.key, itemAccessory)
                }
                if (!isActive) {
                    return@launch
                }
                generateCellModels(item = item, itemCellModelsToUpdate = itemCellModelsToUpdate)
                if (!isActive) {
                    return@launch
                }
                currentProcessingCount++
                if (currentProcessingCount % updateThreshold == 0) {
                    sendChangedToUi(includeItemCellModels = true)
                }
            }
            if (!isActive) {
                return@launch
            }
//            mutableItemCellModels = itemCellModelsToUpdate
            sendChangedToUi(includeItemCellModels = true)
        }

    }

    private fun accessory(item: RItem): ItemAccessory? {
        val attachment = AttachmentCreator.mainAttachment(
            item = item,
            fileStorage = this.fileStore,
            defaults = this.defaults
        )
        if (attachment != null) {
            return ItemAccessory.attachment(attachment)
        }
        val urlString = item.urlString
        if (urlString != null && UrlDetector().isUrl(urlString)) {
            return ItemAccessory.url(urlString)
        }
        val doi = item.doi
        if (doi != null) {
            return ItemAccessory.doi(doi)
        }

        return null
    }

    private fun cellAccessory(accessory: ItemAccessory?): ItemCellModel.Accessory? {
        when (accessory) {
            is ItemAccessory.attachment -> {
                val attachment = accessory.attachment
                val (progress, error) = this.fileDownloader.data(
                    key = attachment.key,
                    libraryId = attachment.libraryId
                )
                return ItemCellModel.Accessory.attachment(
                    State.stateFrom(
                        type = attachment.type,
                        progress = progress,
                        error = error
                    )
                )
            }
            is ItemAccessory.doi ->
                return ItemCellModel.Accessory.doi
            is ItemAccessory.url ->
                return ItemCellModel.Accessory.url
            null -> return null
        }
    }

    fun open(attachment: Attachment, parentKey: String?) {
        val progress =
            this.fileDownloader.data(key = attachment.key, libraryId = attachment.libraryId).first
        if (progress != null) {
            if (this.attachmentToOpen == attachment.key) {
                this.attachmentToOpen = null
            }
            this.fileDownloader.cancel(key = attachment.key, libraryId = attachment.libraryId)
        } else {
            this.attachmentToOpen = attachment.key
            this.fileDownloader.downloadIfNeeded(attachment = attachment, parentKey = parentKey)
        }
    }

    internal fun downloadAttachments(keys: Set<String>) {
        for (key in keys) {
            val progress =
                this.fileDownloader.data(key, libraryId = this.library.identifier).first
            if (progress != null) {
                return
            }
            val attachment = itemAccessories[key]?.attachmentGet ?: return
            this.fileDownloader.downloadIfNeeded(
                attachment = attachment,
                parentKey = if (attachment.key == key) null else key
            )
        }
    }

    internal fun shareDownloads(ids: Set<String>) {
        this.fileShareController.share(
            AttachmentFileShareController.ShareType.allForItems(
                keys = ids,
                libraryId = this.library.identifier
            )
        )
    }

    internal fun removeDownloads(ids: Set<String>) {
        this.fileCleanupController.delete(
            AttachmentFileCleanupController.DeletionType.allForItems(
                keys = ids,
                collectionIdentifier = this.collection.identifier,
                libraryId = this.library.identifier
            ), completed = null
        )
    }

    fun getItemAccessoryByKey(key: String): ItemAccessory? {
        return itemAccessories[key]
    }
    fun getResultByKey(key: String): RItem {
        return this.results!!.first { it.key == key }
    }

    private fun setSortOrder(ascending: Boolean) {
        val sortType = this.sortType.copy(ascending = ascending)
        changeSortType(
            sortType = sortType,
            filters = processorInterface.currentFilters(),
            searchTerm = processorInterface.currentSearchTerm()
        )
    }

    fun getSortType(): ItemsSortType {
        return this.sortType
    }

    fun onSearch(text: String) {
        onSearchStateFlow.tryEmit(text)
    }

    private fun updateDeletedAttachmentFiles(notification: AttachmentFileDeletedNotification) {
        when (notification) {
            is AttachmentFileDeletedNotification.allForItems -> {
                if (collection.identifier != notification.collectionIdentifier) {
                    return
                }
                notification.keys.forEach {
                    updateAttachmentOnNotification(it)
                }
            }
            is AttachmentFileDeletedNotification.individual -> {
                val key = notification.parentKey ?: notification.key
                updateAttachmentOnNotification(key)
            }

            else -> {
                //no-op
            }
        }
    }

    private fun updateAttachmentOnNotification(key: String) {
        val itemAccessory = accessory(getResultByKey(key))
        if (itemAccessory != null) {
            this@AllItemsProcessor.itemAccessories.put(key, itemAccessory)
        }
        updateCellAccessoryForItem(key)
        sendChangedToUi(includeItemCellModels = true)
    }

    fun clear() {
        removeAllListenersFromResultsList()
        this.results = null
        this.resultsProcessorCoroutineScope?.cancel()
        this.resultsProcessorCoroutineScope = null
        this.listenersCoroutineScope.cancel()
        EventBus.getDefault().unregister(this)
    }
}