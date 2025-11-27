package org.zotero.android.screens.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.logging.crash.CrashReportIdDialogData
import org.zotero.android.architecture.logging.crash.CrashShareDataEventStream
import org.zotero.android.architecture.logging.debug.DebugLogging
import org.zotero.android.architecture.logging.debug.DebugLoggingDialogData
import org.zotero.android.architecture.logging.debug.DebugLoggingDialogDataEventStream
import org.zotero.android.architecture.logging.debug.DebugLoggingInterface
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.requests.DeleteGroupDbRequest
import org.zotero.android.database.requests.ReadAllGroupsDbRequest
import org.zotero.android.database.requests.ReadCollectionDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.database.requests.ReadSearchDbRequest
import org.zotero.android.database.requests.groupId
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.InitialLoadData
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.dashboard.data.ShowDashboardLongPressBottomSheet
import org.zotero.android.screens.libraries.data.DeleteGroupDialogData
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SessionController
import org.zotero.android.sync.conflictresolution.AskUserToResolveChangedDeletedItem
import org.zotero.android.sync.conflictresolution.Conflict
import org.zotero.android.sync.conflictresolution.ConflictResolutionUseCase
import org.zotero.android.sync.conflictresolution.ShowSimpleConflictResolutionDialog
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionsHolder
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.webdav.WebDavSessionStorage
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val conflictResolutionUseCase: ConflictResolutionUseCase,
    private val debugLogging: DebugLogging,
    private val debugLoggingDialogDataEventStream: DebugLoggingDialogDataEventStream,
    private val crashShareDataEventStream: CrashShareDataEventStream,
    private val context: Context,
    private val sessionController: SessionController,
    private val sessionStorage: WebDavSessionStorage,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
) : BaseViewModel2<DashboardViewState, DashboardViewEffect>(DashboardViewState()),
    DebugLoggingInterface {

    var isTablet: Boolean = false
    var groupLibraries: RealmResults<RGroup>? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(deleteGroupDialogData: DeleteGroupDialogData) {
        updateState {
            copy(deleteGroupDialogData = deleteGroupDialogData)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AskUserToResolveChangedDeletedItem) {
        updateState {
            copy(changedItemsDeletedAlertQueue = event.conflictDataList.toPersistentList())
        }
    }

    @Subscribe
    fun onEvent(event: ShowDashboardLongPressBottomSheet) {
        updateState {
            copy(
                longPressOptionsHolder = LongPressOptionsHolder(
                    title = event.title,
                    longPressOptionItems = event.longPressOptionItems
                )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: ShowSimpleConflictResolutionDialog) {
        updateState {
            val conflict = event.conflict
            copy(
                conflictDialog = when (conflict) {
                    is Conflict.groupRemoved -> {
                        ConflictDialogData.groupRemoved(
                            conflict.groupId,
                            conflict.name
                        )
                    }
                    is Conflict.groupMetadataWriteDenied -> {
                        ConflictDialogData.groupMetadataWriteDenied(
                            conflict.groupId,
                            conflict.name
                        )
                    }

                    is Conflict.groupFileWriteDenied -> {
                        val domainName: String = if (!sessionStorage.isEnabled) {
                            "zotero.org"
                        } else {
                            sessionStorage.url
                        }

                        ConflictDialogData.groupFileWriteDenied(
                            groupId = conflict.groupId,
                            groupName = conflict.name,
                            domainName = domainName
                        )
                    }
                    is Conflict.objectsRemovedRemotely, is Conflict.removedItemsHaveLocalChanges -> {
                        //no-op
                        null
                    }
                }
            )
        }
    }

    fun init(isTablet: Boolean) = initOnce {
        viewModelScope.launch {
            this@DashboardViewModel.isTablet = isTablet
            EventBus.getDefault().register(this@DashboardViewModel)

            debugLogging.debugLoggingInterface = this@DashboardViewModel
            setupDebugLoggingDialogDataEventStream()
            setupCrashShareDataEventStream()
            listenToGroupDeletionEvents()

            if (sessionController.isInitialized && debugLogging.isEnabled) {
                setDebugWindow(true)
            }

            val data = loadInitialDetailData(
                collectionId = fileStore.getSelectedCollectionIdAsync(),
                libraryId = fileStore.getSelectedLibraryAsync()
            )
            if (data != null) {
                showItems(data.collection, data.library, searchItemKeys = null)
            }
            updateState {
                copy(initialLoadData = data)
            }
        }

    }

    private fun setupDebugLoggingDialogDataEventStream() {
        debugLoggingDialogDataEventStream.flow()
            .onEach { dialogData ->
                updateState {
                    copy(debugLoggingDialogData = dialogData)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun setupCrashShareDataEventStream() {
        crashShareDataEventStream.flow()
            .onEach { dialogData ->
                updateState {
                    copy(crashReportIdDialogData = dialogData)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInitialDetailData(
        collectionId: CollectionIdentifier,
        libraryId: LibraryIdentifier
    ): InitialLoadData? {
        var collection: Collection? = null
        var library: Library? = null

        try {
            dbWrapperMain.realmDbStorage.perform(coordinatorAction = { coordinator ->
                when (collectionId) {
                    is CollectionIdentifier.collection -> {
                        val rCollection = coordinator.perform(
                            request = ReadCollectionDbRequest(
                                libraryId = libraryId,
                                key = collectionId.key
                            )
                        )
                        collection =
                            Collection.initWithCollection(objectS = rCollection, itemCount = 0)
                    }
                    is CollectionIdentifier.search -> {
                        val rSearch = coordinator.perform(
                            request = ReadSearchDbRequest(
                                libraryId = libraryId,
                                key = collectionId.key
                            )
                        )
                        collection = Collection.initWithSearch(objectS = rSearch)
                    }
                    is CollectionIdentifier.custom -> {
                        collection = Collection.initWithCustomType(type = collectionId.type)
                    }
                }
                library = coordinator.perform(request = ReadLibraryDbRequest(libraryId = libraryId))

            })
        } catch (e: Exception) {
            Timber.e(e, "DashboardScreen: can't load initial data")
            return null
        }
        if (collection != null && library != null) {
            return InitialLoadData(collection = collection!!, library = library!!)
        }
        Timber.w("returning default library and collection")
        return InitialLoadData(
            collection = Collection.initWithCustomType(type = CollectionIdentifier.CustomType.all),
            library = Library(
                identifier = LibraryIdentifier.custom(type = RCustomLibraryType.myLibrary),
                name = "My Library",
                metadataEditable = true,
                filesEditable = true
            )
        )
    }

    private fun showItems(collection: Collection, library: Library, searchItemKeys: List<String>?) {
        val searchTerm = searchItemKeys?.joinToString(separator = " ")
        ScreenArguments.allItemsArgs = AllItemsArgs(
            collection = collection,
            library = library,
            searchTerm = searchTerm,
            error = null
        )
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun deleteRemovedItemsWithLocalChanges(key: String) {
        conflictResolutionUseCase.deleteRemovedItemsWithLocalChanges(key)
        maybeCompleteRemovedItemsWithLocalChanges(key)
    }

    fun restoreRemovedItemsWithLocalChanges(key: String) {
        conflictResolutionUseCase.restoreRemovedItemsWithLocalChanges(key)
        maybeCompleteRemovedItemsWithLocalChanges(key)
    }

    private fun maybeCompleteRemovedItemsWithLocalChanges(key: String) {
        updateState {
            copy(changedItemsDeletedAlertQueue = viewState.changedItemsDeletedAlertQueue.filter { it.key != key }.toPersistentList())
        }
        if (viewState.changedItemsDeletedAlertQueue.isEmpty()) {
            conflictResolutionUseCase.completeRemovedItemsWithLocalChanges()
        }
    }

    fun deleteGroup(key: Int) {
        conflictResolutionUseCase.deleteGroup(key)
    }
    fun markGroupAsLocalOnly(key: Int) {
        conflictResolutionUseCase.markGroupAsLocalOnly(key)
    }
    fun revertGroupChanges(key: Int) {
        conflictResolutionUseCase.revertGroupChanges(key)
    }

    fun revertGroupFiles(groupId: Int) {
        conflictResolutionUseCase.revertGroupFiles(LibraryIdentifier.group(groupId))
    }
    fun skipGroup(groupId: Int) {
        conflictResolutionUseCase.skipGroup(LibraryIdentifier.group(groupId))
    }

    fun onDismissConflictDialog() {
        updateState {
            copy(
                conflictDialog = null,
            )
        }
    }

    fun onDismissDebugLoggingDialog() {
        updateState {
            copy(
                debugLoggingDialogData = null,
            )
        }
    }

    fun onDismissCrashLoggingDialog() {
        updateState {
            copy(
                crashReportIdDialogData = null,
            )
        }
    }

    fun onDismissDeleteGroupDialog() {
        updateState {
            copy(
                deleteGroupDialogData = null,
            )
        }
    }

    fun dismissBottomSheet() {
        updateState {
            copy(longPressOptionsHolder = null)
        }
    }

    fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        EventBus.getDefault().post(longPressOptionItem)
    }

    fun onContentReadingRetry(logs: List<File>, userId: Long, customAlertMessage: ((String) -> String)?) {
        debugLogging.onContentReadingRetry(logs, userId, customAlertMessage)
    }

    fun onContentReadingOk() {
        debugLogging.onContentReadingOk()
    }

    fun onUploadRetry(logs: List<File>, userId: Long, customAlertMessage: ((String) -> String)?) {
        debugLogging.onUploadRetry(logs, userId, customAlertMessage)
    }

    fun onUploadOk() {
        debugLogging.onUploadOk()
    }

    fun onShareCopy(debugId: String) {
        val clipboardManager: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Identifier", debugId)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun setDebugWindow(visible: Boolean) {
        viewModelScope.launch {
            updateState {
                copy(showDebugWindow = visible)
            }
        }
    }

    fun onDebugStop() {
        debugLogging.stop()
    }

    fun deleteNonLocalGroup(groupId: Int) {
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                DeleteGroupDbRequest(groupId = groupId)
            ).ifFailure {
                Timber.e(it, "DashboardViewModel: can't delete group")
                return@launch
            }
        }
    }

    private fun listenToGroupDeletionEvents() {
        dbWrapperMain.realmDbStorage.perform { coordinator ->
            this.groupLibraries = coordinator.perform(request = ReadAllGroupsDbRequest())

            this.groupLibraries?.addChangeListener { _, changeSet ->
                when (changeSet.state) {
                    OrderedCollectionChangeSet.State.INITIAL -> {
                        //no-op
                    }

                    OrderedCollectionChangeSet.State.UPDATE -> {
                        val deletions = changeSet.deletions
                        if (deletions.isNotEmpty()) {
                            showDefaultLibraryIfNeeded()
                        }
                    }

                    OrderedCollectionChangeSet.State.ERROR -> {
                        Timber.e(changeSet.error, "DashboardViewModel: could not listen to Group Events")
                    }
                    else -> {
                        //no-op
                    }
                }
            }
        }
    }

    private fun showDefaultLibraryIfNeeded() {
        when (val visibleLibraryId = fileStore.getSelectedLibrary()) {
            is LibraryIdentifier.custom -> {
                //no-op
            }

            is LibraryIdentifier.group -> {
                val groupId = visibleLibraryId.groupId

                if (this.groupLibraries?.where()?.groupId(groupId)?.findFirst() == null) {
                    showCollections(LibraryIdentifier.custom(RCustomLibraryType.myLibrary))
                }
            }
        }
    }

    fun showCollections(libraryId: LibraryIdentifier) {
        val collectionId = storeIfNeeded(libraryId = libraryId)

        val collectionsArgs = CollectionsArgs(
            libraryId = libraryId,
            selectedCollectionId = collectionId,
            shouldRecreateItemsScreen = this.isTablet
        )
        val encodedArgs = navigationParamsMarshaller.encodeObjectToBase64(collectionsArgs, StandardCharsets.UTF_8)
        triggerEffect(DashboardViewEffect.NavigateToCollectionsScreen(encodedArgs))
    }

    private fun storeIfNeeded(libraryId: LibraryIdentifier, collectionId: CollectionIdentifier? = null): CollectionIdentifier {
        if (fileStore.getSelectedLibrary() == libraryId) {
            if (collectionId != null) {
                fileStore.setSelectedCollectionId(collectionId)
                return collectionId
            }
            return fileStore.getSelectedCollectionId()
        }

        val collectionId = collectionId ?: CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
        fileStore.setSelectedLibrary(libraryId)
        fileStore.setSelectedCollectionId(collectionId)
        return collectionId

    }

    suspend fun getInitialCollectionArgs(): String {
        val collectionsArgs = CollectionsArgs(
            libraryId = fileStore.getSelectedLibraryAsync(),
            selectedCollectionId = fileStore.getSelectedCollectionIdAsync()
        )
        val encodedArgs =
            navigationParamsMarshaller.encodeObjectToBase64Async(collectionsArgs, StandardCharsets.UTF_8)
        return encodedArgs
    }
}

data class DashboardViewState(
    val initialLoadData: InitialLoadData? = null,
    val snackbarMessage: SnackbarMessage? = null,
    val conflictDialog: ConflictDialogData? = null,
    val debugLoggingDialogData: DebugLoggingDialogData? = null,
    val crashReportIdDialogData: CrashReportIdDialogData? = null,
    val deleteGroupDialogData: DeleteGroupDialogData? = null,
    val changedItemsDeletedAlertQueue: PersistentList<ConflictDialogData.changedItemsDeletedAlert> = persistentListOf(),
    val longPressOptionsHolder: LongPressOptionsHolder? = null,
    val showDebugWindow: Boolean = false,
    ) : ViewState

sealed class DashboardViewEffect : ViewEffect {
    data class NavigateToCollectionsScreen(val screenArgs:String) : DashboardViewEffect()
}

sealed class ConflictDialogData  {
    data class groupRemoved(val groupId: Int, val groupName: String): ConflictDialogData()
    data class groupMetadataWriteDenied(val groupId: Int, val groupName: String): ConflictDialogData()
    data class changedItemsDeletedAlert(val title: String, val key: String): ConflictDialogData()
    data class groupFileWriteDenied(val groupId: Int, val groupName: String, val domainName: String) : ConflictDialogData()

}