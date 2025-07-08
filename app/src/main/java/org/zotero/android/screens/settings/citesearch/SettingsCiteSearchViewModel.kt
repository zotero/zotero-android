package org.zotero.android.screens.settings.citesearch

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.androidx.content.longToast
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
import org.zotero.android.screens.settings.ARG_SETTINGS_CITE_SEARCH
import org.zotero.android.screens.settings.citesearch.data.SettingsCiteSearchArgs
import org.zotero.android.styles.data.RemoteStyle
import org.zotero.android.styles.data.RemoteStyleMapper
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SettingsCiteSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val remoteStyleMapper: RemoteStyleMapper,
    private val nonZoteroApi: NonZoteroApi,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
    private val context: Context,
) : BaseViewModel2<SettingsCiteSearchViewState, SettingsCiteSearchViewEffect>(
    SettingsCiteSearchViewState()
) {

    private val screenArgs: SettingsCiteSearchArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_SETTINGS_CITE_SEARCH).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private val onSearchStateFlow = MutableStateFlow("")

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
                filter(text)
            }
            .launchIn(viewModelScope)
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
                val filtered = remoteStyles.filter { !installedIds.contains(it.id) }
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
            updateState {
                copy(filtered = null)
            }
            return
        }
        val filtered = viewState.styles.filter { style ->
            style.title.contains(string, ignoreCase = true)
                    || style.category.fields.any { it.contains(string, ignoreCase = true) }
        }
        updateState {
            copy(filtered = filtered)
        }
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }


    override fun onCleared() {
        super.onCleared()
    }

    fun onItemTapped(style: RemoteStyle) {


    }

}

internal data class SettingsCiteSearchViewState(
    val styles: List<RemoteStyle> = emptyList(),
    val filtered: List<RemoteStyle>? = null,
    val lce: LCE2 = LCE2.Loading,
    val searchTerm: String? = null,
) : ViewState {
}

internal sealed class SettingsCiteSearchViewEffect : ViewEffect {
    object OnBack : SettingsCiteSearchViewEffect()
}