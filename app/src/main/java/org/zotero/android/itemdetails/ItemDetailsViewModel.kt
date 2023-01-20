package org.zotero.android.itemdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.ObjectChangeSet
import io.realm.RealmObjectChangeListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.joda.time.DateTime
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.UpdatableChangeType
import org.zotero.android.architecture.database.requests.EditItemFromDetailDbRequest
import org.zotero.android.architecture.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.architecture.database.requests.ReadItemDbRequest
import org.zotero.android.architecture.ifFailure
import org.zotero.android.dashboard.data.CreatorEditArgs
import org.zotero.android.dashboard.data.DetailType
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.ItemDetailData
import org.zotero.android.dashboard.data.ItemDetailError
import org.zotero.android.dashboard.data.ItemDetailField
import org.zotero.android.dashboard.data.ShowItemDetailsArgs
import org.zotero.android.files.FileStore
import org.zotero.android.formatter.dateAndTimeFormat
import org.zotero.android.formatter.fullDateWithDashes
import org.zotero.android.formatter.iso8601DateFormatV2
import org.zotero.android.formatter.sqlFormat
import org.zotero.android.sync.ItemDetailDataCreator
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
internal class ItemDetailsViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val urlDetector: UrlDetector,
    private val schemaController: SchemaController,
    private val stateHandle: SavedStateHandle,
    private val dispatcher: CoroutineDispatcher,
) : BaseViewModel2<ItemDetailsViewState, ItemDetailsViewEffect>(ItemDetailsViewState()) {

    private var coroutineScope = CoroutineScope(dispatcher)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(itemDetailCreator: ItemDetailCreator) {
        onSaveCreator(itemDetailCreator)
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)

        val args = ScreenArguments.showItemDetailsArgs

        initViewState(args)
        process(ItemDetailAction.reloadData)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private fun initViewState(args: ShowItemDetailsArgs) {
        val type = args.type
        val library = args.library
        val preScrolledChildKey = args.childKey
        val userId = defaults.getUserId()

        when (type) {
            is DetailType.preview -> {
                updateState {
                    copy(
                        key = type.key,
                        isEditing = false)
                }

            }
            is DetailType.creation, is DetailType.duplication -> {
                updateState {
                    copy(
                        key = KeyGenerator.newKey(),
                        isEditing = true,
                    )
                }

            }

        }
        updateState {
            copy(
                type = type,
                userId = userId,
                library = library,
                preScrolledChildKey = preScrolledChildKey)
        }

    }

    fun onSaveOrEditClicked() {
        if (viewState.isEditing) {
            saveChanges()
        } else {
            val updatedStateData = viewState.data.deepCopy()
            val updatedData = viewState.data.deepCopy(
                fieldIds = ItemDetailDataCreator.allFieldKeys(
                    viewState.data.type,
                    schemaController = this.schemaController
                )
            )
            updateState {
                copy(
                    snapshot = updatedStateData,
                    data = updatedData,
                    isEditing = true,
                )
            }
        }


    }

    private fun process(action: ItemDetailAction) {
        when (action) {
            ItemDetailAction.loadInitialData -> loadInitialData()
            ItemDetailAction.reloadData -> reloadData(viewState.isEditing)
        }
    }

    private fun loadInitialData() {
        val key = viewState.key
        val libraryId = viewState.library!!.identifier
        var collectionKey: String?

        try {
            when (viewState.type) {
                is DetailType.preview -> {
                    reloadData(isEditing = viewState.isEditing)
                    return
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "can't load initial data ")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
            return
        }
    }

    private fun reloadData(isEditing: Boolean) = viewModelScope.launch {
        try {
            val item = dbWrapper.realmDbStorage.perform(request = ReadItemDbRequest(libraryId = viewState.library!!.identifier, key = viewState.key), refreshRealm = true)

            item.addChangeListener(RealmObjectChangeListener<RItem> { items, changeSet ->
                if(changeSet?.changedFields?.any { RItem.observableKeypathsForItemDetail.contains(it) } == true) {
                    itemChanged(items, changeSet)
                }
            }
            )

            var (data, attachments, notes, tags) = ItemDetailDataCreator.createData(
                ItemDetailDataCreator.Kind.existing(item = item, ignoreChildren = false),
                schemaController = this@ItemDetailsViewModel.schemaController,
                fileStorage = this@ItemDetailsViewModel.fileStore, urlDetector = this@ItemDetailsViewModel.urlDetector,
                doiDetector = {doiValue -> FieldKeys.Item.isDoi(doiValue)})

            if (!isEditing) {
                data.fieldIds = ItemDetailDataCreator.filteredFieldKeys(data.fieldIds, fields = data.fields)
            }

            saveReloaded(data = data, attachments = attachments, notes = notes, tags = tags, isEditing = isEditing)
        } catch (e: Exception) {
            Timber.e(e,"ItemDetailActionHandler: can't load data")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
        }
    }

    private fun saveReloaded(
        data: ItemDetailData,
        attachments: List<Attachment>,
        notes: List<Note>,
        tags: List<Tag>,
        isEditing: Boolean,
    ) {
        updateState {
            copy(data = data)
        }
        if (viewState.snapshot != null || isEditing) {
            val updatedSnapshot = data.deepCopy(
                fieldIds = ItemDetailDataCreator.filteredFieldKeys(
                    data.fieldIds,
                    fields = data.fields
                )
            )
            updateState {
                copy(
                    snapshot = updatedSnapshot,
                )
            }
        }
        updateState {
            copy(
                attachments = attachments,
                notes = notes,
                tags = tags,
                isLoadingData = false,
                isEditing = isEditing,
            )
        }
        //Update UI and hide progress.
    }

    private fun itemChanged(item: RItem, changeSet: ObjectChangeSet) {
        if (changeSet.isDeleted) {
            return
        }

        if (shouldReloadData(item, changeSet.changedFields)) {
            itemChanged(viewState)
        }
    }

    private fun shouldReloadData(item: RItem, changes: Array<String>): Boolean {
        if (changes.contains("version")) {
            //Use old value?
            if (changes.contains("changeType") && item.changeType != UpdatableChangeType.user.name) {
                return true
            }
            return false
        }

        if (changes.contains("children")) {
            return true
        }
        return false
    }

    //Start of Changes
    private fun itemChanged(state: ItemDetailsViewState) {
        if (!state.isEditing) {
            process(ItemDetailAction.reloadData)
            return
        }
        //TODO show Data Reloaded dialog for editing case
    }
    //End Of Changes

    fun onAddCreator() {
        showCreatorCreation(itemType = viewState.data.type)
    }

    fun showCreatorCreation(itemType: String) {
        val schema = schemaController.creators(itemType)?.firstOrNull { it.primary }
        if (schema == null) {
            return
        }
        val localized = schemaController.localizedCreator(schema.creatorType)
        if (localized == null) {
            return
        }
        val creator = ItemDetailCreator.init(
            type = schema.creatorType, primary = schema.primary,
            localizedType = localized, namePresentation = defaults.getCreatorNamePresentation())
        _showCreatorEditor(creator, itemType = itemType)
    }

    fun showCreatorEditor(creator: ItemDetailCreator, itemType: String) {
        _showCreatorEditor(creator, itemType = itemType)
    }
    private fun _showCreatorEditor(creator: ItemDetailCreator, itemType: String) {
        ScreenArguments.creatorEditArgs = CreatorEditArgs(creator = creator, itemType = itemType)
        triggerEffect(ItemDetailsViewEffect.ShowCreatorEditEffect)
    }

    private fun onSaveCreator(creator: ItemDetailCreator) {
        if (!viewState.data.creatorIds.contains(creator.id)) {
            updateState {
                copy(
                    data = viewState.data.deepCopy(
                        creatorIds = viewState.data.creatorIds + creator.id,
                        creators = viewState.data.creators + (creator.id to creator)
                    )
                )
            }
        }
    }
    fun setFieldValue(id: String, value: String) {
        val field = viewState.data.fields[id]
        if (field == null) {
            return
        }

        field.value = value
        field.isTappable = ItemDetailDataCreator.isTappable(
            key = field.key,
            value = field.value,
            urlDetector = this.urlDetector,
            doiDetector = { doiValue -> FieldKeys.Item.isDoi(doiValue) }
        )

        if (field.key == FieldKeys.Item.date || field.baseField == FieldKeys.Item.date) {
            //TODO parse order
        } else if (field.additionalInfo != null) {
            field.additionalInfo = null
        }

        val updatedData = viewState.data.deepCopy(fields = viewState.data.fields)
        updateState {
            copy(data = updatedData)
        }
        triggerEffect(ItemDetailsViewEffect.ScreenRefresh)
    }

    fun onTitleEdit(newTitle: String) {
        val updatedData = viewState.data.deepCopy(title = newTitle)
        updateState {
            copy(data = updatedData)
        }
    }

    fun onAbstractEdit(newAbstract: String) {
        val updatedData = viewState.data.deepCopy(abstract = newAbstract)
        updateState {
            copy(data = updatedData)
        }
    }

    private fun parseDateSpecialValue(value: String): Date? {
        when (value.lowercase()) {
            "tomorrow" ->
                return DateTime.now().plusDays(1).toDate()
            "today" ->
                return Date()
            "yesterday" ->
                return DateTime.now().minusDays(1).toDate()
            else ->
                return null
        }
    }

    private fun updateDateFieldIfNeeded() {
        val field = viewState.data.fields.values.firstOrNull { it.baseField == FieldKeys.Item.date || it.key == FieldKeys.Item.date }
        if (field != null) {
            val date = parseDateSpecialValue(field.value)
            if (date != null) {
                field.value = fullDateWithDashes.format(date)
                //TODO parse date order

                val updatedFieldsMap = viewState.data.fields.toMutableMap()
                updatedFieldsMap[field.key] = field

                val updatedData = viewState.data.deepCopy(fields = updatedFieldsMap)
                updateState {
                    copy(data = updatedData)
                }
            }
        }
    }

    fun saveChanges() {
        if (viewState.snapshot == viewState.data) {
            return
        }
        try {
            coroutineScope.launch {
                save()
            }
        } catch (error: Exception) {
            Timber.e(error, "ItemDetailStore: can't store changes")
            updateState {
                copy(error = error as? ItemDetailError ?: ItemDetailError.cantStoreChanges)
            }
        }
    }

    private fun save() {
        updateDateFieldIfNeeded()
        updateAccessedFieldIfNeeded()

        viewModelScope.launch {
            updateState {
                copy(data = viewState.data.deepCopy(dateModified = Date()))
            }
        }

        val snapshot = viewState.snapshot
        if (snapshot != null) {
            val request = EditItemFromDetailDbRequest(
                libraryId = viewState.library!!.identifier,
                itemKey = viewState.key,
                data = viewState.data,
                snapshot = snapshot,
                schemaController = this.schemaController
            )
            dbWrapper.realmDbStorage.perform(request = request)
        }
        val updatedData = viewState.data.deepCopy(
            fieldIds = ItemDetailDataCreator.filteredFieldKeys(
                viewState.data.fieldIds,
                viewState.data.fields
            )
        )
        viewModelScope.launch {
            updateState {
                copy(
                    snapshot = null,
                    isEditing = false,
                    type = DetailType.preview(viewState.key),
                    data = updatedData
                )
            }
        }

    }

    private fun updateAccessedFieldIfNeeded() {
        var field = viewState.data.fields[FieldKeys.Item.accessDate]
        if (field == null) {
            return
        }
        var date: Date? = null
        val dateSpecific = parseDateSpecialValue(field.value)
        if (dateSpecific != null) {
            date = dateSpecific
        } else {
            try {
                val _date = sqlFormat.parse(field.value)
                if (_date != null) {
                    date = _date
                }
            } catch (e: Exception) {
                //no-op
            }

        }

        if (date != null) {
            field.value = iso8601DateFormatV2.format(date)
            field.additionalInfo = mapOf(ItemDetailField.AdditionalInfoKey.formattedDate to dateAndTimeFormat.format(date),
                ItemDetailField.AdditionalInfoKey.formattedEditDate to sqlFormat.format(date))
        } else {
            val snapshotField = viewState.snapshot?.fields?.get(FieldKeys.Item.accessDate)
            if (snapshotField != null) {
                field = snapshotField
            } else {
                field.value = ""
                field.additionalInfo = emptyMap()
            }
        }

        val updatedFieldsMap = viewState.data.fields.toMutableMap()
        updatedFieldsMap[field.key] = field

        val updatedData = viewState.data.deepCopy(fields = updatedFieldsMap)
        viewModelScope.launch {
            updateState {
                copy(data = updatedData)
            }
        }
    }

    fun onCancelOrBackClicked() {
        if (viewState.isEditing) {
            cancelChanges()
        } else {
            triggerEffect(ItemDetailsViewEffect.OnBack)
        }
    }

    private fun cancelChanges() {
        viewModelScope.launch {
            val type = viewState.type
            if (type is DetailType.duplication) {
                val result = perform(
                    dbWrapper = dbWrapper,
                    request = MarkObjectsAsDeletedDbRequest(
                        clazz = RItem::class,
                        keys = listOf(viewState.key),
                        libraryId = viewState.library!!.identifier
                    )
                ).ifFailure {
                    Timber.e(it, "ItemDetailActionHandler: can't remove duplicated item")
                    updateState {
                        copy(error = ItemDetailError.cantRemoveDuplicatedItem)
                    }
                    return@launch
                }
                return@launch

            }
        }

        val snapshot = viewState.snapshot?.deepCopy()
        if (snapshot == null) {
            return
        }
        updateState {
            copy(
                data = snapshot,
                snapshot = null,
                isEditing = false
            )
        }
    }

}

internal data class ItemDetailsViewState(
    val key : String = "",
    val library : Library? = null,
    val userId : Long = 0,
    val type: DetailType? = null,
    val preScrolledChildKey: String? = null,
    val isEditing: Boolean = false,
    var error: ItemDetailError? = null,
    val data: ItemDetailData = ItemDetailData.empty,
    var snapshot: ItemDetailData? = null,
    var notes: List<Note> = emptyList(),
    var attachments: List<Attachment> = emptyList(),
    var tags: List<Tag> = emptyList(),
    var isLoadingData: Boolean = false,
) : ViewState

internal sealed class ItemDetailsViewEffect : ViewEffect {
    object ShowCreatorEditEffect: ItemDetailsViewEffect()
    object ScreenRefresh: ItemDetailsViewEffect()
    object OnBack: ItemDetailsViewEffect()
}

sealed class ItemDetailAction {
    object loadInitialData: ItemDetailAction()
    object reloadData: ItemDetailAction()
}