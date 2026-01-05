package org.zotero.android.screens.settings.citesearch

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.zotero.android.androidx.content.longToast
import org.zotero.android.androidx.content.toast
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.network.safeApiCall
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.requests.InstallStyleDbRequest
import org.zotero.android.database.requests.StoreStyleDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.screens.settings.ARG_SETTINGS_CITE_SEARCH
import org.zotero.android.screens.settings.citesearch.data.SettingsCitSearchStyleDownloadedEventStream
import org.zotero.android.screens.settings.citesearch.data.SettingsCiteSearchArgs
import org.zotero.android.styles.data.RemoteStyle
import org.zotero.android.styles.data.RemoteStyleMapper
import org.zotero.android.styles.data.Style
import org.zotero.android.styles.data.StylesParser
import timber.log.Timber
import java.io.File
import java.net.URL
import javax.inject.Inject

@HiltViewModel
internal class SettingsCiteSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val remoteStyleMapper: RemoteStyleMapper,
    private val nonZoteroApi: NonZoteroApi,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
    private val context: Context,
    private val dbWrapperBundle: DbWrapperBundle,
    private val fileStore: FileStore,
    private val settingsCitSearchStyleDownloadedEventStream: SettingsCitSearchStyleDownloadedEventStream,
) : BaseViewModel2<SettingsCiteSearchViewState, SettingsCiteSearchViewEffect>(
    SettingsCiteSearchViewState()
) {

    private val screenArgs: SettingsCiteSearchArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_SETTINGS_CITE_SEARCH).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private val onSearchStateFlow = MutableStateFlow("")

    private val coroutineScope = CoroutineScope(dispatchers.io)
    private val syncSchedulerSemaphore = Semaphore(1)

    fun init() = initOnce {
        viewModelScope.launch {
            setupSearchStateFlow()
            load()
        }
    }

    private fun setupSearchStateFlow() {
        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                syncSchedulerSemaphore.withPermit {
                    filter(text)
                }
            }
            .launchIn(coroutineScope)
    }

    private suspend fun load() {
        val installedIds = screenArgs.installedIds
        updateState {
            copy(lce = LCE2.Loading)
        }

        try {
            withContext(dispatchers.io) {
                val networkResult = safeApiCall {
                    nonZoteroApi.stylesRequest()
                }
                if (networkResult is CustomResult.GeneralError.NetworkError) {
                    throw Exception(networkResult.stringResponse)
                }
                networkResult as CustomResult.GeneralSuccess.NetworkSuccess
                val remoteStyles = remoteStyleMapper.fromJson(networkResult.value!!)
                val filtered =
                    remoteStyles.filter { !installedIds.contains(it.id) }.toMutableStateList()
                viewModelScope.launch {
                    updateState {
                        copy(
                            lce = LCE2.Content,
                            styles = filtered
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Can't load styles")
            updateState {
                copy(lce = LCE2.Content)
            }
            context.longToast("Can't load styles")
        }
    }

    private fun filter(string: String) {
        if (string.isEmpty()) {
            viewModelScope.launch {
                updateState {
                    copy(filtered = null)
                }
            }

            return
        }
        val filtered = viewState.styles.filter { style ->
            style.title.contains(string, ignoreCase = true)
                    || style.category.fields.any { it.contains(string, ignoreCase = true) }
        }.toMutableStateList()
        viewModelScope.launch {
            updateState {
                copy(filtered = filtered)
            }
        }
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }


    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }

    fun onItemTapped(style: RemoteStyle) {
        viewModelScope.launch {
            context.toast("Adding new style...")
            installOrAdd(style)
        }
    }

    private suspend fun installOrAdd(style: RemoteStyle) = withContext(dispatchers.io) {
        val result = dbWrapperBundle.realmDbStorage.perform(
            InstallStyleDbRequest(identifier = style.id),
            invalidateRealm = true
        )
        if (result) {
            return@withContext
        }
        add(remoteStyle = style)
    }

    private suspend fun add(remoteStyle: RemoteStyle) {
        val file = fileStore.style(remoteStyle.name)

        val networkResult = safeApiCall {
            nonZoteroApi.downloadFileStreaming(url = remoteStyle.href, headers = emptyMap())
        }
        when (networkResult) {
            is CustomResult.GeneralSuccess -> {
                try {
                    val byteArray = networkResult.value!!.bytes()
                    FileHelper.writeByteArrayToFile(file, byteArray)
                    val style = loadStyle(file)
                    if (style != null) {
                        process(style = style)
                    } else {
                        Timber.e("can't parse downloaded style")
                        viewModelScope.launch {
                            context.longToast("Unable to parse style")
                        }
                        file.delete()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "can't download style")
                    processUnableToDownloadStyleError(file)
                }
            }

            is CustomResult.GeneralError.CodeError -> {
                Timber.e(networkResult.throwable, "can't download style")
                processUnableToDownloadStyleError(file)
            }

            is CustomResult.GeneralError.NetworkError -> {
                Timber.e("can't download style: ${networkResult.stringResponse}")
                processUnableToDownloadStyleError(file)
            }
        }

    }

    private suspend fun process(style: Style) {
        val dependencyUrl = style.dependencyId?.let {
            try {
                URL(it)
                it
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
        if (dependencyUrl == null) {
            _add(style = style, dependency = null)
            return
        }

        val file = fileStore.style(dependencyUrl.substringAfterLast('/'))

        val networkResult = safeApiCall {
            nonZoteroApi.downloadFileStreaming(url = dependencyUrl, headers = emptyMap())
        }
        when (networkResult) {
            is CustomResult.GeneralSuccess -> {
                try {
                    val byteArray = networkResult.value!!.bytes()
                    FileHelper.writeByteArrayToFile(file, byteArray)

                    val dependency = loadStyle(file)

                    if (dependency == null) {
                        _add(style = style, dependency = null)
                        return
                    }
                    _add(style = style, dependency = dependency)
                } catch (e: Exception) {
                    Timber.e(e, "can't download style")
                    processUnableToDownloadStyleError(file)
                }
            }

            is CustomResult.GeneralError.CodeError -> {
                Timber.e(networkResult.throwable, "can't download style")
                processUnableToDownloadStyleError(file)
            }

            is CustomResult.GeneralError.NetworkError -> {
                Timber.e("can't download style: ${networkResult.stringResponse}")
                processUnableToDownloadStyleError(file)
            }
        }
    }

    private fun _add(style: Style, dependency: Style?) {
        try {
            dbWrapperBundle.realmDbStorage.perform(
                StoreStyleDbRequest(
                    style = style,
                    dependency = dependency
                )
            )
            settingsCitSearchStyleDownloadedEventStream.emitAsync(Unit)
            viewModelScope.launch {
                triggerEffect(SettingsCiteSearchViewEffect.OnBack)
                context.toast("New style was added")
            }
        } catch (e: Exception) {
            Timber.e(e, "can't store style")
            viewModelScope.launch {
                context.longToast("Can't store style")
            }
        }
    }

    private fun processUnableToDownloadStyleError(file: File) {
        viewModelScope.launch {

            file.delete()
            context.longToast("Unable to download style")
        }

    }

    private fun loadStyle(file: File): Style? {
        val parser = StylesParser.fromFile(file)

        val style = parser.parseXml()
        if (style != null) {
            return style
        }
        return null
    }

}

internal data class SettingsCiteSearchViewState(
    val styles: SnapshotStateList<RemoteStyle> = mutableStateListOf(),
    val filtered: SnapshotStateList<RemoteStyle>? = null,
    val lce: LCE2 = LCE2.Loading,
    val searchTerm: String? = null,
) : ViewState

internal sealed class SettingsCiteSearchViewEffect : ViewEffect {
    object OnBack : SettingsCiteSearchViewEffect()
}