package org.zotero.android.screens.settings.debug

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.logging.debug.DebugLogging
import javax.inject.Inject

@HiltViewModel
internal class SettingsDebugViewModel @Inject constructor(
    private val debugLogging: DebugLogging,
) : BaseViewModel2<SettingsDebugViewState, SettingsDebugViewEffect>(SettingsDebugViewState()) {

    fun init() = initOnce {
        debugLogging.logLines
            .map { numberOfLines ->
                updateState {
                    copy(numberOfLines = numberOfLines, isLogging = debugLogging.isEnabled)
                }
            }
            .launchIn(viewModelScope)

        updateState {
            copy(isLogging = debugLogging.isEnabled, numberOfLines = debugLogging.logLines.value)
        }
    }

    fun startLogging() {
        debugLogging.start(type = DebugLogging.LoggingType.immediate)
        updateState {
            copy(isLogging = true)
        }
    }

    fun startLoggingOnNextAppLaunch() {
        debugLogging.start(type = DebugLogging.LoggingType.nextLaunch)
    }

    fun stopLogging() {
        debugLogging.stop()
        updateState {
            copy(
                isLogging = false,
                numberOfLines = 0
            )
        }
    }

    fun clearLogs() {
        debugLogging.cancel {
            viewModelScope.launch {
                updateState {
                    copy(numberOfLines = 0)
                }
            }

            debugLogging.start(type = DebugLogging.LoggingType.immediate)
        }
    }
    fun cancelLogging() {
        debugLogging.cancel()
        updateState {
            copy(
                isLogging = false,
                numberOfLines = 0
            )
        }

    }

}

internal data class SettingsDebugViewState(
    val numberOfLines: Int = 0,
    val isLogging: Boolean = false
) : ViewState

internal sealed class SettingsDebugViewEffect : ViewEffect {
    object OnBack : SettingsDebugViewEffect()
}