package org.zotero.android.screens.share

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadCollectionAndLibraryDbRequest
import org.zotero.android.database.requests.ReadRecentCollections
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.screens.share.data.CollectionPickerState
import org.zotero.android.screens.share.data.ProcessedAttachment
import org.zotero.android.screens.share.data.RecentData
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncObservableEventStream
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.translator.web.TranslatorWebCallChainExecutor
import org.zotero.android.translator.web.TranslatorWebExtractionExecutor
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ShareViewModel @Inject constructor(
    dispatchers: Dispatchers,
    private val stateHandle: SavedStateHandle,
    private val translatorWebExtractionExecutor: TranslatorWebExtractionExecutor,
    private val translatorWebCallChainExecutor: TranslatorWebCallChainExecutor,
    private val shareRawAttachmentLoader: ShareRawAttachmentLoader,
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    private val fileStore: FileStore,
    private val syncScheduler: SyncScheduler,
    private val syncObservableEventStream: SyncObservableEventStream,
    private val dbWrapper: DbWrapper,
) : BaseViewModel2<ShareViewState, ShareViewEffect>(ShareViewState()) {

    private val defaultLibraryId: LibraryIdentifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
    private val defaultExtension = "pdf"
    private val defaultMimetype = "application/pdf"
    private val zipMimetype = "application/zip"

    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    private lateinit var selectedCollectionId: CollectionIdentifier
    private lateinit var selectedLibraryId: LibraryIdentifier
    private lateinit var attachmentKey: String

    fun init() = initOnce {
        selectedCollectionId = fileStore.getSelectedCollectionId()
        selectedLibraryId = fileStore.getSelectedLibrary()
        attachmentKey = KeyGenerator.newKey()
        setupSyncObserving()
        ioCoroutineScope.launch {
            try {
                syncScheduler.startSyncController(type = SyncKind.collectionsOnly, libraries = Libraries.all, retryAttempt = 0)
                val attachment = shareRawAttachmentLoader.loadAttachment(this@ShareViewModel.stateHandle)
                process(attachment)
            } catch (e: Exception) {
                Timber.e(e, "ExtensionViewModel: could not load attachment")
                updateAttachmentState(
                    AttachmentState.failed(
                        attachmentError(
                            generalError = CustomResult.GeneralError.CodeError(e),
                            libraryId = null
                        )
                    )
                )
            }
        }
    }

    private fun setupSyncObserving() {
        syncObservableEventStream.flow()
            .onEach { data ->
                finishSync(successful = (data == null))
            }
            .launchIn(viewModelScope)


    }

    private fun finishSync(successful: Boolean) {
        if (!successful) {
            updateState {
                copy(collectionPickerState = CollectionPickerState.failed)
            }
            return
        }


        try {
            var library: Library? = null
            var collection: Collection? = null
            var recents = mutableListOf<RecentData>()
            dbWrapper.realmDbStorage.perform { coordinator ->
                val request = ReadCollectionAndLibraryDbRequest(
                    collectionId = this.selectedCollectionId,
                    libraryId = this.selectedLibraryId
                )
                val (_collection, _library) = coordinator.perform(request = request)

                val recentCollections =
                    coordinator.perform(request = ReadRecentCollections(excluding = null))

                recents = recentCollections.toMutableList()
                library = _library
                when (this.selectedCollectionId) {
                    is CollectionIdentifier.collection -> {
                        collection = _collection
                    }

                    else -> {
                        //no-op
                    }
                }
            }
            if (library == null) {
                return
            }

            if (!recents.any { it.collection?.identifier == collection?.identifier && it.library.identifier == library!!.identifier }) {
                recents.add(
                    index = 0,
                    element = RecentData(
                        collection = collection,
                        library = library!!,
                        isRecent = false
                    )
                )
            }

            updateState {
                copy(
                    collectionPickerState = CollectionPickerState.picked(
                        library = library!!,
                        collection = collection
                    ),
                    recents = recents
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "can't load collections")
            val library = Library(
                identifier = this.defaultLibraryId,
                name = RCustomLibraryType.myLibrary.libraryName,
                metadataEditable = true,
                filesEditable = true
            )
            updateState {
                copy(
                    collectionPickerState = CollectionPickerState.picked(
                        library = library,
                        collection = null
                    )
                )
            }
        }
    }

    private suspend fun process(url: String) {
        try {
            val attachment = translatorWebExtractionExecutor.execute(url = url)
            process(attachment)
        } catch (e: Exception) {
            Timber.e(e, "ExtensionViewModel: webview could not load data")
            updateAttachmentState(
                AttachmentState.failed(
                    attachmentError(
                        generalError = CustomResult.GeneralError.CodeError(e),
                        libraryId = null
                    )
                )
            )
        }
    }

    private suspend fun process(attachment: RawAttachment) {
        when (attachment) {
            is RawAttachment.web -> {
                processWeb(
                    title = attachment.title,
                    url = attachment.url,
                    html = attachment.html,
                    cookies = attachment.cookies,
                    frames = attachment.frames,
                    userAgent = attachment.userAgent,
                    referrer = attachment.referrer
                )
            }
            is RawAttachment.remoteUrl -> {
                process(url = attachment.url)
            }
            is RawAttachment.fileUrl -> {
                process(uri = attachment.uri)
            }
            else -> {

            }
        }
    }

    private suspend fun process(uri: Uri) {
        val fileName = getUriDetailsUseCase.getFullName(uri)
        val fileExtension =  getUriDetailsUseCase.getExtension(uri)
        if (fileName == null || fileExtension == null) {
            updateState {
                copy(attachmentState = AttachmentState.failed(AttachmentState.Error.fileMissing))
            }
            return
        }
        val tmpFile = fileStore.temporaryFile(fileExtension)
        try {
            getUriDetailsUseCase.copyFile(uri, tmpFile)
            updateState {
                copy(
                    processedAttachment = ProcessedAttachment.file(
                        file = tmpFile,
                        fileName = fileName
                    ),
                    expectedAttachment = fileName to tmpFile,
                    attachmentState = AttachmentState.processed
                )
            }
        } catch (e: Exception) {
            updateState {
                copy(attachmentState = AttachmentState.failed(AttachmentState.Error.fileMissing))
            }
        }
    }

    private fun processWeb(
        title: String,
        url: String,
        html: String,
        cookies: String,
        frames: List<String>,
        userAgent: String,
        referrer: String
    ) {
        viewModelScope.launch {
            updateState {
                copy(
                    title = title,
                    url = url
                )
            }
        }

        translatorWebCallChainExecutor.translate(
            url = url,
            html = html,
            cookies = cookies,
            frames = frames,
            userAgent = userAgent,
            referrer = referrer
        )

    }

    private fun updateAttachmentState(attachmentState: AttachmentState) {
        viewModelScope.launch {
            updateState {
                copy(attachmentState = attachmentState)
            }
        }
    }

    private fun attachmentError(
        generalError: CustomResult.GeneralError,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        when (generalError) {
            is CustomResult.GeneralError.CodeError -> {
                val error = generalError.throwable
                if (error is AttachmentState.Error) {
                    return error
                }
                if (error is Parsing.Error) {
                    Timber.e(error, "ExtensionViewModel: could not parse item")
                    return AttachmentState.Error.parseError(error)
                }

                if (error is SchemaError) {
                    Timber.e(error, "ExtensionViewModel: schema failed")
                    return AttachmentState.Error.schemaError(error)
                }
                if (error is TranslationWebViewError) {
                    return AttachmentState.Error.webViewError(error)
                }
            }

            is CustomResult.GeneralError.NetworkError -> {
                return networkErrorRequiresAbort(
                    error = generalError,
                    url = generalError.httpUrl?.toUrl()?.toString(),
                    libraryId = libraryId
                )
            }
        }
        return AttachmentState.Error.unknown
    }

    private fun networkErrorRequiresAbort(
        error: CustomResult.GeneralError.NetworkError,
        url: String?,
        libraryId: LibraryIdentifier?
    ): AttachmentState.Error {
        val defaultError = if ((url ?: "").contains(BuildConfig.BASE_API_URL)) {
            AttachmentState.Error.apiFailure
        } else {
            AttachmentState.Error.webDavFailure
        }

        val code = error.httpCode
        if (code == 413  && libraryId != null) {
            return AttachmentState.Error.quotaLimit(libraryId)
        }
        return defaultError
    }


}

internal data class ShareViewState(
    val title: String? = null,
    val url: String? = null,
    val attachmentState: AttachmentState = AttachmentState.decoding,
    val expectedAttachment: Pair<String, File>? = null,
    val processedAttachment: ProcessedAttachment? = null,
    val collectionPickerState: CollectionPickerState = CollectionPickerState.loading,
    val recents: List<RecentData> = emptyList()
) : ViewState

internal sealed class ShareViewEffect : ViewEffect {
    object NavigateBack : ShareViewEffect()
}
