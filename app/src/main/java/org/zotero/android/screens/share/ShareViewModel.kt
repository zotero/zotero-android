package org.zotero.android.screens.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zotero.android.BuildConfig
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Parsing
import org.zotero.android.sync.SchemaError
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import org.zotero.android.translator.data.TranslationWebViewError
import org.zotero.android.translator.web.TranslatorWebCallChainExecutor
import org.zotero.android.translator.web.TranslatorWebExtractionExecutor
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ShareViewModel @Inject constructor(
    dispatchers: Dispatchers,
    private val stateHandle: SavedStateHandle,
    private val translatorWebExtractionExecutor: TranslatorWebExtractionExecutor,
    private val translatorWebCallChainExecutor: TranslatorWebCallChainExecutor,
    private val shareRawAttachmentLoader: ShareRawAttachmentLoader,
) : BaseViewModel2<ShareViewState, ShareViewEffect>(ShareViewState()) {

    private val ioCoroutineScope = CoroutineScope(dispatchers.io)

    fun init() = initOnce {
        ioCoroutineScope.launch {
            try {
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
//                self.process(fileUrl: url)
            }
            else -> {

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

    private fun attachmentError(generalError: CustomResult.GeneralError, libraryId: LibraryIdentifier?): AttachmentState.Error {
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
    var title: String? = null,
    var url: String? = null,
    val attachmentState: AttachmentState = AttachmentState.decoding
) : ViewState

internal sealed class ShareViewEffect : ViewEffect {
    object NavigateBack : ShareViewEffect()
}
