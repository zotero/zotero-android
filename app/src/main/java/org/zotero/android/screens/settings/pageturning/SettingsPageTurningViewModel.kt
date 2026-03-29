package org.zotero.android.screens.settings.pageturning

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
import org.zotero.android.styles.data.Style
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SettingsPageTurningViewModel @Inject constructor(
    private val defaults: Defaults,
) : BaseViewModel2<SettingsPageTurningViewState, SettingsPageTurningViewEffect>(SettingsPageTurningViewState()) {

    fun init() = initOnce {
        viewModelScope.launch {
            updateState {
                copy(
                    pageTurning = defaults.isButtonPageTurning(),
                    keepZoom = defaults.isButtonPageTurning()
                )
            }
        }
    }

    fun onBack() {
        triggerEffect(SettingsPageTurningViewEffect.OnBack)
    }

    fun onButtonPageTurningSwitchTapped(bool: Boolean) {
        updateState {
            copy(pageTurning = bool)
        }
        defaults.setButtonPageTurning(bool)
    }

    fun onButtonKeepZoomSwitchTapped(bool: Boolean) {
        updateState {
            copy(keepZoom = bool)
        }
        defaults.setKeepZoom(bool)
    }

}

internal data class SettingsPageTurningViewState(
    val styles: PersistentList<Style> = persistentListOf(),
    val pageTurning: Boolean = false,
    val keepZoom: Boolean = false,
) : ViewState

internal sealed class SettingsPageTurningViewEffect : ViewEffect {
    object OnBack : SettingsPageTurningViewEffect()
}