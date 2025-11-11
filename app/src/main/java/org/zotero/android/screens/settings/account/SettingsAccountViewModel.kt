package org.zotero.android.screens.settings.account

import android.content.Context
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgress
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgressEventStream
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.DeleteAllWebDavDeletionsDbRequest
import org.zotero.android.database.requests.MarkAttachmentsNotUploadedDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.root.RootActivity
import org.zotero.android.screens.settings.account.SettingsAccountViewEffect.NavigateToSinglePickerScreen
import org.zotero.android.screens.settings.account.SettingsAccountViewEffect.OnBack
import org.zotero.android.screens.settings.account.SettingsAccountViewEffect.OpenWebpage
import org.zotero.android.screens.settings.account.data.CreateWebDavDirectoryDialogData
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Libraries
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SessionController
import org.zotero.android.sync.SyncError
import org.zotero.android.sync.SyncKind
import org.zotero.android.sync.SyncScheduler
import org.zotero.android.uicomponents.singlepicker.SinglePickerArgs
import org.zotero.android.uicomponents.singlepicker.SinglePickerItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerState
import org.zotero.android.webdav.WebDavController
import org.zotero.android.webdav.WebDavSessionStorage
import org.zotero.android.webdav.data.FileSyncType
import org.zotero.android.webdav.data.WebDavError
import org.zotero.android.webdav.data.WebDavScheme
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class SettingsAccountViewModel @Inject constructor(
    private val defaults: Defaults,
    private val sessionController: SessionController,
    private val sessionStorage: WebDavSessionStorage,
    private val webDavController: WebDavController,
    private val fileStore: FileStore,
    private val context: Context,
    private val dbWrapperMain: DbWrapperMain,
    private val syncScheduler: SyncScheduler,
    private val syncProgressEventStream: SyncProgressEventStream,
    private val dispatchers: Dispatchers,
) : BaseViewModel2<SettingsAccountViewState, SettingsAccountViewEffect>(SettingsAccountViewState()) {

    private var coroutineScope = CoroutineScope(dispatchers.io)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.SettingsWebDav) {
            viewModelScope.launch {
                val scheme = WebDavScheme.valueOf(singlePickerResult.id)
                setScheme(scheme = scheme)
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val isVerified = sessionStorage.isVerified

        updateState {
            copy(
                account = defaults.getUsername(),
                fileSyncType = if (sessionStorage.isEnabled) {
                    FileSyncType.webDav
                } else {
                    FileSyncType.zotero
                },
                scheme = sessionStorage.scheme,
                url = sessionStorage.url,
                username = sessionStorage.username,
                password = sessionStorage.password,
                webDavVerificationResult = if (isVerified) {
                    CustomResult.GeneralSuccess(Unit)
                } else {
                    null
                }
            )
        }
    }

    fun onBack() {
        triggerEffect(OnBack)
    }

    fun openDeleteAccount() {
        val url = "https://www.zotero.org/settings/deleteaccount"
        triggerEffect(OpenWebpage(url))
    }

    fun openManageAccount() {
        val url = "https://www.zotero.org/settings/account"
        triggerEffect(OpenWebpage(url))

    }

    fun onSignOut() {
        sessionController.reset()
        context.startActivity(RootActivity.getIntentClearTask(context))
    }

    fun dismissWebDavOptionsDialog() {
        updateState {
            copy(
                showWebDavOptionsDialog = false
            )
        }
    }

    fun showWebDavOptionsPopup() {
        updateState {
            copy(
                showWebDavOptionsDialog = true
            )
        }
    }

    fun setFileSyncType(type: FileSyncType) {
        dismissWebDavOptionsDialog()
        if (viewState.fileSyncType == type) {
            return
        }

        syncScheduler.cancelSync()

        val oldType = viewState.fileSyncType
        updateState {
            copy(
                fileSyncType = type,
                markingForReupload = true
            )
        }

        markAttachmentsForReupload(type) { error ->
            updateState {
                var res = this
                if (error != null) {
                    res = res.copy(fileSyncType = oldType)
                }
                res.copy(markingForReupload = false)
            }
            if (error != null) {
                return@markAttachmentsForReupload
            }

            sessionStorage.isEnabled = type == FileSyncType.webDav

            if (type == FileSyncType.zotero) {
                if (syncScheduler.inProgress.value) {
                    syncScheduler.cancelSync()
                }
                syncScheduler.request(SyncKind.normal, Libraries.all)
            }
        }
    }

    fun setUrl(url: String) {
        if (viewState.url == url) {
            return
        }
        var decodedUrl = url
        if (url.contains("%")) {
            decodedUrl = HtmlCompat.fromHtml(
                url,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString()
        }
        sessionStorage.url = decodedUrl
        webDavController.resetVerification()

        updateState {
            copy(
                url = url,
                webDavVerificationResult = null,
                markingForReupload = true
            )
        }

        markAttachmentsForReupload(FileSyncType.webDav) {
            updateState {
                copy(markingForReupload = false)
            }
        }
    }

    private fun markAttachmentsForReupload(type: FileSyncType, completion: (Exception?) -> Unit) {
        try {
            performMark(type)
            completion(null)
        } catch (error: Exception) {
            Timber.e(error, "SettingsAccountViewModel: can't mark all attachments not uploaded")
            completion(error)
        }
    }

    fun performMark(type: FileSyncType) {
        val keys = downloadedAttachmentKeys()
        val requests = mutableListOf<DbRequest>(
            MarkAttachmentsNotUploadedDbRequest(
                keys = keys,
                libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
            )
        )
        if (type == FileSyncType.zotero) {
            requests.add(DeleteAllWebDavDeletionsDbRequest())
        }
        dbWrapperMain.realmDbStorage.perform(requests)
    }

    private fun downloadedAttachmentKeys(): List<String> {
        val contents =
            fileStore.downloads(LibraryIdentifier.custom(RCustomLibraryType.myLibrary)).listFiles()
                ?: emptyArray()
        return contents.filter { file ->
            val fullPath = file.absolutePath
            val partOfPath = fullPath.substring(fullPath.indexOf("downloads"))
            val relativeComponentsCount = partOfPath.count { it == File.separatorChar }
            val lastPathPart = partOfPath.substring(partOfPath.lastIndexOf(File.separatorChar) + 1)

            if (relativeComponentsCount == 2 && (lastPathPart
                    ?: "").length == KeyGenerator.length
            ) {
                val contents = file.list() ?: emptyArray()
                return@filter contents.isNotEmpty()

            }
            false
        }.map {
            val fullPath = it.absolutePath
            fullPath.substring(fullPath.lastIndexOf(File.separatorChar) + 1)
        }
    }

    fun setScheme(scheme: WebDavScheme) {
        if (viewState.scheme == scheme) {
            return
        }
        updateState {
            copy(scheme = scheme, webDavVerificationResult = null)
        }
        sessionStorage.scheme = scheme
        webDavController.resetVerification()
    }

    fun setUsername(username: String) {
        if (viewState.username == username) {
            return
        }
        updateState {
            copy(username = username, webDavVerificationResult = null)
        }
        sessionStorage.username = username
        webDavController.resetVerification()
    }

    fun setPassword(password: String) {
        if (viewState.password == password) {
            return
        }
        updateState {
            copy(password = password, webDavVerificationResult = null)
        }

        sessionStorage.password = password
        webDavController.resetVerification()
    }

    fun cancelVerification() {
        cancelProcessing()
        updateState {
            copy(isVerifyingWebDav = false)
        }
    }

    fun recheckKeys() {
        if (syncScheduler.inProgress.value) {
            syncScheduler.cancelSync()
        }
        observeSyncIssues()
        syncScheduler.request(SyncKind.keysOnly, Libraries.all)
    }

    private fun observeSyncIssues() {
        syncProgressEventStream.flow()
            .onEach { progress ->
                process(progress = progress)
            }
            .launchIn(viewModelScope)
    }

    private fun process(progress: SyncProgress) {
        when (progress) {
            is SyncProgress.aborted -> {
                val fatalError = progress.error
                if (fatalError is SyncError.Fatal.forbidden) {
                    sessionController.reset()
                }
                cancelProcessing()

            }
            is SyncProgress.finished -> {
                cancelProcessing()
            }

            else -> {
                //no-op
            }
        }
    }

    fun showSchemaChooserScreen() {
        val pickerState = createSinglePickerState()
        ScreenArguments.singlePickerArgs =
            SinglePickerArgs(
                singlePickerState = pickerState,
                callPoint = SinglePickerResult.CallPoint.SettingsWebDav,
            )
        triggerEffect(NavigateToSinglePickerScreen)
    }

    fun verify() {
        verify(tryCreatingZoteroDir = false)
    }

    private fun verify(tryCreatingZoteroDir: Boolean) {
        if (viewState.scheme == WebDavScheme.http && !isAllowedHttpHost()) {
            updateState {
                copy(
                    webDavVerificationResult = CustomResult.GeneralError.CodeError(WebDavError.Verification.localHttpWebdavHostNotAllowed),
                    isVerifyingWebDav = false
                )
            }
            return
        }

        if (!viewState.isVerifyingWebDav) {
            updateState {
                copy(isVerifyingWebDav = true)
            }
        }

        coroutineScope.launch {
            if (tryCreatingZoteroDir) {
                val createZoteroDirectoryResult = webDavController.createZoteroDirectory()
                if (createZoteroDirectoryResult is CustomResult.GeneralError) {
                    handleVerification(error = createZoteroDirectoryResult)
                    return@launch
                }
            }

            val checkServerResult = webDavController.checkServer()
            if (checkServerResult is CustomResult.GeneralError) {
                handleVerification(error = checkServerResult)
                return@launch
            }
            viewModelScope.launch {
                updateState {
                    copy(
                        isVerifyingWebDav = false,
                        webDavVerificationResult = CustomResult.GeneralSuccess(Unit)
                    )
                }
            }

            if (syncScheduler.inProgress.value) {
                syncScheduler.cancelSync()
            }
            syncScheduler.request(type = SyncKind.normal, libraries = Libraries.all)

        }

    }

    private fun isAllowedHttpHost(): Boolean {
        try {
            val hostComponentsWithPort = sessionStorage.url.split(":")
            val hostComponentsWithSlashes = hostComponentsWithPort.firstOrNull()?.split("/")
            val host = hostComponentsWithSlashes?.firstOrNull()
            if (host != null && (host.endsWith("local") || host.endsWith("home.arpa"))) {
                return true
            }
        } catch (e: Exception) {
            //no-op
        }
        return false
    }

    private fun handleVerification(error: CustomResult.GeneralError) {
        viewModelScope.launch {
            val zoteroDirNotFoundError =
                (error as? CustomResult.GeneralError.CodeError)?.throwable as? WebDavError.Verification.zoteroDirNotFound
            if (zoteroDirNotFoundError != null) {
                updateState {
                    copy(
                        createWebDavDirectoryDialogData = CreateWebDavDirectoryDialogData(
                            url = zoteroDirNotFoundError.url,
                            error = error
                        )
                    )
                }
                return@launch
            }

            updateState {
                copy(
                    webDavVerificationResult = error,
                    isVerifyingWebDav = false
                )
            }
        }

    }

    private fun createSinglePickerState(
    ): SinglePickerState {
        val items = listOf(
            SinglePickerItem(id = WebDavScheme.https.name, name = WebDavScheme.https.name),
            SinglePickerItem(id = WebDavScheme.http.name, name = WebDavScheme.http.name),
        )
        val state = SinglePickerState(objects = items, selectedRow = viewState.scheme.name)
        return state
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private fun cancelProcessing() {
        this.coroutineScope.cancel()
        this.coroutineScope  = CoroutineScope(dispatchers.io)
    }

    fun onDismissCreateDirectoryDialog(error: CustomResult.GeneralError) {
        updateState {
            copy(
                createWebDavDirectoryDialogData = null,
                webDavVerificationResult = error,
                isVerifyingWebDav = false,
            )
        }
    }

    fun onShowSignOutDialog() {
        updateState {
            copy(
                shouldShowSignOutDialog = true,
            )
        }
    }

    fun onDismissSignOutDialog() {
        updateState {
            copy(
                shouldShowSignOutDialog = false,
            )
        }
    }

    fun onCreateWebDavDirectory() {
        verify(tryCreatingZoteroDir = true)
    }
}

internal data class SettingsAccountViewState(
    val account: String = "",
    val showWebDavOptionsDialog: Boolean = false,
    val fileSyncType: FileSyncType = FileSyncType.zotero,
    val markingForReupload: Boolean = false,
    val scheme: WebDavScheme = WebDavScheme.https,
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val isVerifyingWebDav: Boolean = false,
    val webDavVerificationResult: CustomResult<Unit>? = null,
    val createWebDavDirectoryDialogData: CreateWebDavDirectoryDialogData? = null,
    val shouldShowSignOutDialog: Boolean = false
) : ViewState {
    val canVerifyServer: Boolean get() {
        return !url.isEmpty() && !username.isEmpty() && !password.isEmpty()
    }
}

internal sealed class SettingsAccountViewEffect : ViewEffect {
    object OnBack : SettingsAccountViewEffect()
    object NavigateToSinglePickerScreen : SettingsAccountViewEffect()
    data class OpenWebpage(val url: String) : SettingsAccountViewEffect()
}