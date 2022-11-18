package org.zotero.android.dashboard

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.api.pojo.sync.ItemResponse
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.dashboard.data.DetailType
import org.zotero.android.dashboard.data.ItemDetailError
import org.zotero.android.dashboard.data.ShowItemDetailsArgs
import org.zotero.android.sync.ItemResultsUseCase
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ItemDetailsViewModel @Inject constructor(
    stateHandle: SavedStateHandle,
    private val sdkPrefs: SdkPrefs,
    private val itemResultsUseCase: ItemResultsUseCase,
) : BaseViewModel2<ItemDetailsViewState, ItemDetailsViewEffect>(ItemDetailsViewState()) {

    fun init() = initOnce {
        val args = ScreenArguments.showItemDetailsArgs

        initViewState(args)
        process(ItemDetailAction.reloadData)
    }

    fun initViewState(args: ShowItemDetailsArgs) {
        val type = args.type
        val library = args.library
        val preScrolledChildKey = args.childKey
        val userId = sdkPrefs.getUserId()

        when (type) {
            is DetailType.preview -> {
                updateState {
                    copy(
                        key = key,
                        isEditing = false)
                }

            }
            is DetailType.creation, is DetailType.duplication -> {
                updateState {
                    copy(
                        key = KeyGenerator.newKey,
                        isEditing = true,
                    )
                }

            }

        }
        updateState {
            copy(
                initialType = type,
                userId = userId,
                library = library,
                preScrolledChildKey = preScrolledChildKey)
        }

    }

    private fun process(action: ItemDetailAction) {
        when (action) {
            ItemDetailAction.loadInitialData -> loadInitialData()
            ItemDetailAction.reloadData -> TODO()
        }
    }

    private fun loadInitialData() {
        val key = viewState.key
        val libraryId = viewState.library!!.identifier
        var collectionKey: String?

        try {
            when (viewState.initialType) {
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

    private fun reloadData(isEditing: Boolean) {


    }

}

internal data class ItemDetailsViewState(
    val itemResponse: ItemResponse? = null,
    val initialType : DetailType? = null,
    val key : String = "",
    val library : Library? = null,
    val userId : Long = 0,
    val preScrolledChildKey: String? = null,
    val isEditing: Boolean = false,
    var error: ItemDetailError? = null
) : ViewState

internal sealed class ItemDetailsViewEffect : ViewEffect {
}

sealed class ItemDetailAction {
    object loadInitialData: ItemDetailAction()
    object reloadData: ItemDetailAction()
}