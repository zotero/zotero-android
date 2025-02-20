package org.zotero.android.screens.retrievemetadata

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.ARG_RETRIEVE_METADATA
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.pdfworker.PdfWorkerController
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataArgs
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import javax.inject.Inject

@HiltViewModel
internal class RetrieveMetadataViewModel @Inject constructor(
    stateHandle: SavedStateHandle,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val pdfWorkerController: PdfWorkerController,
) : BaseViewModel2<RetrieveMetadataViewState, RetrieveMetadataViewEffect>(RetrieveMetadataViewState()) {

    val screenArgs: RetrieveMetadataArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_RETRIEVE_METADATA).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    fun init() = initOnce {
        val itemKey = screenArgs.itemKey
        val libraryId = screenArgs.libraryId

        pdfWorkerController.observable.flow()
            .onEach { result ->
                observe(result)
            }
            .launchIn(viewModelScope)

        pdfWorkerController.recognizeExistingItem(itemKey = itemKey, libraryId = libraryId)
    }

    private fun observe(update: PdfWorkerController.Update) {
        when(update) {
            is PdfWorkerController.Update.recognizeInit -> {
                updateState {
                    copy(pdfFileName = update.pdfFileName)
                }
            }
            PdfWorkerController.Update.recognizedDataIsEmpty -> {
                updateState {
                    copy(retrieveMetadataState = RetrieveMetadataState.recognizedDataIsEmpty)
                }
            }
            is PdfWorkerController.Update.recognizeError -> {
                updateState { copy(retrieveMetadataState = RetrieveMetadataState.failed(update.errorMessage)) }
            }

            is PdfWorkerController.Update.recognizedAndSaved -> {
                updateState {
                    copy(
                        retrieveMetadataState = RetrieveMetadataState.success(
                            recognizedTitle = update.recognizedTitle,
                            recognizedTypeIcon = "" //no-op
                        )
                    )
                }
            }

            is PdfWorkerController.Update.recognizedAndKeptInMemory -> {
                //no-op
            }
        }
    }

    override fun onCleared() {
        pdfWorkerController.cancelAllLookups()
        super.onCleared()
    }

}

internal data class RetrieveMetadataViewState(
    val pdfFileName: String = "",
    val retrieveMetadataState: RetrieveMetadataState = RetrieveMetadataState.loading,
) : ViewState

internal sealed class RetrieveMetadataViewEffect : ViewEffect {
    object NavigateBack : RetrieveMetadataViewEffect()
}
