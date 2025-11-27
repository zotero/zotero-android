package org.zotero.android.screens.settings.cite

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.database.DbWrapperBundle
import org.zotero.android.database.RealmDbCoordinator
import org.zotero.android.database.requests.ReadInstalledStylesDbRequest
import org.zotero.android.database.requests.ReadStyleDbRequest
import org.zotero.android.database.requests.ReadStylesDbRequest
import org.zotero.android.database.requests.UninstallStyleDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.dashboard.data.ShowDashboardLongPressBottomSheet
import org.zotero.android.screens.settings.citesearch.data.SettingsCitSearchStyleDownloadedEventStream
import org.zotero.android.screens.settings.citesearch.data.SettingsCiteSearchArgs
import org.zotero.android.styles.data.Style
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SettingsCiteViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val dbWrapperBundle: DbWrapperBundle,
    private val defaults: Defaults,
    private val fileStore: FileStore,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val settingsCitSearchStyleDownloadedEventStream: SettingsCitSearchStyleDownloadedEventStream,
) : BaseViewModel2<SettingsCiteViewState, SettingsCiteViewEffect>(SettingsCiteViewState()) {

    fun init() = initOnce {
        setupSettingsCitSearchStyleDownloadedEventStream()
        viewModelScope.launch {
            EventBus.getDefault().register(this@SettingsCiteViewModel)
            reloadStyles()
        }
    }

    private fun setupSettingsCitSearchStyleDownloadedEventStream() {
        settingsCitSearchStyleDownloadedEventStream.flow()
            .onEach { update ->
                reloadStyles()
            }
            .launchIn(viewModelScope)
    }

    private suspend fun reloadStyles() {
        val styles = loadStyles()
        updateState {
            copy(styles = styles.toPersistentList())
        }
    }

    private suspend fun loadStyles(): List<Style> = withContext(dispatchers.io) {
        val rStyles = dbWrapperBundle.realmDbStorage.perform(ReadInstalledStylesDbRequest())
        val styles = rStyles.mapNotNull { Style.fromRStyle(it) }
        styles
    }

    private fun remove(style: Style) {
        var toRemove = listOf<String>()

        dbWrapperBundle.realmDbStorage.perform { coordinator ->
            toRemove =
                coordinator.perform(request = UninstallStyleDbRequest(identifier = style.identifier))
            resetDefaultStylesIfNeeded(style = style, coordinator = coordinator)
            coordinator.invalidate()
        }

        for (identifier in toRemove) {
            fileStore.style(filenameWithoutExtension = identifier).delete()
        }
    }

    private fun resetDefaultStylesIfNeeded(style: Style, coordinator: RealmDbCoordinator) {
        val quickCopyRemoved = style.identifier == defaults.getQuickCopyStyleId()
        val exportRemoved = style.identifier == defaults.getExportStyleId()

        if (!quickCopyRemoved && !exportRemoved) {
            return
        }

        val resetRemoved: (String) -> Unit = { newId ->
            if (quickCopyRemoved) {
                defaults.setQuickCopyStyleId(newId)
            }
            if (exportRemoved) {
                defaults.setExportStyleId(newId)
            }
        }
        try {
            val defaultStyle =
                coordinator.perform(request = ReadStyleDbRequest(identifier = "http://www.zotero.org/styles/chicago-notes-bibliography"))
            resetRemoved(defaultStyle.identifier)
            return
        } catch (e: Throwable) {
            print(e)
        }
        try {
            val availableStyle = coordinator.perform(request = ReadStylesDbRequest()).first()
            resetRemoved(availableStyle!!.identifier)
            return
        } catch (e: Throwable) {
            print(e)
        }
        resetRemoved("")
    }


    private fun onDelete(style: Style) {
        try {
            remove(style = style)
        } catch (e: Exception) {
            Timber.e(e, "CiteActionHandler: can't delete style ${style.id}")
            return
        }
        val index = viewState.styles.indexOfFirst { it.identifier == style.identifier }
        val stylesMutable = viewState.styles.toMutableList()
        stylesMutable.removeAt(index)
        updateState {
            copy(styles = stylesMutable.toPersistentList())
        }
    }

    fun onBack() {
        triggerEffect(SettingsCiteViewEffect.OnBack)
    }

    fun onItemLongTapped(style: Style) {
        EventBus.getDefault().post(
            ShowDashboardLongPressBottomSheet(
                title = style.title,
                longPressOptionItems = listOf(
                    LongPressOptionItem.CiteStyleDelete(style),
                )
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LongPressOptionItem) {
        onLongPressOptionsItemSelected(event)
    }

    private fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        viewModelScope.launch {
            when (longPressOptionItem) {
                is LongPressOptionItem.CiteStyleDelete -> {
                    onDelete(longPressOptionItem.style)
                }

                else -> {}
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    fun navigateToCiteSearch() {
        val installedIds = viewState.styles.map { it.identifier }.toSet()

        val args =
            SettingsCiteSearchArgs(installedIds = installedIds)
        val params = navigationParamsMarshaller.encodeObjectToBase64(args)
        triggerEffect(SettingsCiteViewEffect.NavigateToCiteSearch(params))
    }

}

internal data class SettingsCiteViewState(
    val styles: PersistentList<Style> = persistentListOf(),
) : ViewState

internal sealed class SettingsCiteViewEffect : ViewEffect {
    object OnBack : SettingsCiteViewEffect()
    data class NavigateToCiteSearch(val args: String) : SettingsCiteViewEffect()
}