package org.zotero.android.screens.libraries

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibrary
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RGroup
import org.zotero.android.database.requests.ReadAllCustomLibrariesDbRequest
import org.zotero.android.database.requests.ReadAllGroupsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.libraries.data.DeleteGroupDialogData
import org.zotero.android.screens.libraries.data.LibraryRowData
import org.zotero.android.screens.libraries.data.LibraryState
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
internal class LibrariesViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val fileStore: FileStore,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
) : BaseViewModel2<LibrariesViewState, LibrariesViewEffect>(LibrariesViewState()) {

    var customLibraries: RealmResults<RCustomLibrary>? = null
    var groupLibraries: RealmResults<RGroup>? = null
    var isTablet: Boolean = false

    fun init(isTablet: Boolean) = initOnce {
        this.isTablet = isTablet
        viewModelScope.launch {
            loadData()
        }
    }

    private fun loadData() {
        dbWrapperMain.realmDbStorage.perform { coordinator ->
            val libraries = coordinator.perform(request = ReadAllCustomLibrariesDbRequest())
            val groups = coordinator.perform(request = ReadAllGroupsDbRequest())

            groups.addChangeListener { _, changeSet ->
                when (changeSet.state) {
                    OrderedCollectionChangeSet.State.INITIAL -> {
                        //no-op
                    }

                    OrderedCollectionChangeSet.State.UPDATE -> {
                        generateLibraryRows()
                    }

                    OrderedCollectionChangeSet.State.ERROR -> {
                        Timber.e(changeSet.error, "LibrariesViewModel: could not load results")
                    }
                    else -> {
                        //no-op
                    }
                }
            }
            this.groupLibraries = groups
            this.customLibraries = libraries
            generateLibraryRows()
        }
    }

    private fun generateLibraryRows() {
        updateState {
            copy(
                customLibraries = this@LibrariesViewModel.customLibraries?.map {
                    createCustomLibraryRowData(it)
                }?.toImmutableList() ?: persistentListOf(),
                groupLibraries = this@LibrariesViewModel.groupLibraries?.map {
                    createGroupLibraryRowData(it)
                }?.toImmutableList() ?: persistentListOf(),
                lce = LCE2.Content
            )
        }
    }

    private fun createCustomLibraryRowData(library: RCustomLibrary): LibraryRowData {
        return LibraryRowData(
            id = -1,
            name = RCustomLibraryType.valueOf(library.type).libraryName,
            state = LibraryState.normal
        )
    }
    private fun createGroupLibraryRowData(library: RGroup): LibraryRowData {
        val state: LibraryState
        if (library.isLocalOnly) {
            state = LibraryState.archived
        } else if (!library.canEditMetadata) {
            state = LibraryState.locked
        } else {
            state = LibraryState.normal
        }
        return LibraryRowData(id = library.identifier ,name = library.name, state = state)
    }

    private fun libraryForCustomLibrary(index: Int): Library? {
        val library = this.customLibraries?.get(index) ?: return null
        return Library(customLibrary = library)
    }

    private fun libraryForGroupLibrary(index: Int): Library? {
        val library = this.groupLibraries?.get(index) ?: return null
        return Library(group = library)
    }

    override fun onCleared() {
        groupLibraries?.removeAllChangeListeners()
        super.onCleared()
    }

    fun onCustomLibraryTapped(index: Int) {
        val lib = libraryForCustomLibrary(index)
        if(lib != null) {
            showCollections(lib.identifier)
        }
    }

    fun onGroupLibraryTapped(index: Int) {
        val lib = libraryForGroupLibrary(index)
        if(lib != null) {
            showCollections(lib.identifier)
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
        triggerEffect(LibrariesViewEffect.NavigateToCollectionsScreen(encodedArgs))
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

    fun showDeleteGroupQuestion(id: Int, name: String) {
        dismissDeleteGroupPopup()
        EventBus.getDefault().post(DeleteGroupDialogData(id = id, name = name))
    }

    fun dismissDeleteGroupPopup() {
        updateState {
            copy(
                groupIdForDeletePopup = null,
            )
        }
    }

    fun showDeleteGroupPopup(item: LibraryRowData) {
        val group = this.groupLibraries!!.find { it.identifier == item.id } ?: return
        if (!group.isLocalOnly) {
            return
        }
        updateState {
            copy(
                groupIdForDeletePopup = group.identifier,
            )
        }
    }

}

internal data class  LibrariesViewState(
    val str: String = "",
    val lce: LCE2 = LCE2.Loading,
    val groupIdForDeletePopup: Int? = null,
    val customLibraries: ImmutableList<LibraryRowData> = persistentListOf(),
    val groupLibraries: ImmutableList<LibraryRowData> = persistentListOf(),
) : ViewState {

}

internal sealed class  LibrariesViewEffect : ViewEffect {
    data class NavigateToCollectionsScreen(val screenArgs: String) : LibrariesViewEffect()
}
