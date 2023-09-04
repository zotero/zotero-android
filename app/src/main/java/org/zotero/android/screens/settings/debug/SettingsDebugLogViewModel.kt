package org.zotero.android.screens.settings.debug

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.logging.debug.DebugLogging
import javax.inject.Inject

@HiltViewModel
internal class SettingsDebugLogViewModel @Inject constructor(
    private val debugLogging: DebugLogging,
) : BaseViewModel2<SettingsDebugLogViewState,
        SettingsDebugLogViewEffect>(SettingsDebugLogViewState()) {

    fun init() = initOnce {
        debugLogging.logLines
            .map { numberOfLines ->
                updateState {
                    copy(numberOfLines = numberOfLines)
                }
            }
            .launchIn(viewModelScope)

        debugLogging.logString
            .map { log ->
                updateState {
                    copy(log = log)
                }
            }
            .launchIn(viewModelScope)

        updateState {
            copy(
                log = debugLogging.logString.value,
                numberOfLines = debugLogging.logLines.value
            )
        }
    }

}

internal data class SettingsDebugLogViewState(
    val numberOfLines: Int = 0,
    val log: String = ""
) : ViewState

internal sealed class SettingsDebugLogViewEffect : ViewEffect {
    object OnBack : SettingsDebugViewEffect()
}