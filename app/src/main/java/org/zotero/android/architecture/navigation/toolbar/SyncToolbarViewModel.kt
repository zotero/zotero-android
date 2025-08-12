package org.zotero.android.architecture.navigation.toolbar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.toolbar.data.CurrentSyncProgressState
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgress
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgressEventStream
import org.zotero.android.sync.SyncError
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

@HiltViewModel
class SyncToolbarViewModel @Inject constructor(
    private val syncProgressEventStream: SyncProgressEventStream,
    private val syncToolbarTextGenerator: SyncToolbarTextGenerator,
) : BaseViewModel2<SyncToolbarViewState, SyncToolbarViewEffect>(SyncToolbarViewState()) {

    private val finishVisibilityTime = 4_000L
    private var timer: Timer? = null

    private var pendingErrors: List<Exception>? = null
    private var muteProgressDialogsOnSpecificScreens = false

    fun init() = initOnce {
        setupSyncProgressEventStream()
    }

    private fun setupSyncProgressEventStream() {
        syncProgressEventStream.flow()
            .onEach { syncProgress ->
                update(syncProgress)
            }
            .launchIn(viewModelScope)
    }

    private fun update(progress: SyncProgress) {
        this.pendingErrors = null
        when (progress) {
            is SyncProgress.aborted -> {
                when (progress.error) {
                    SyncError.Fatal.cancelled -> {
                        this.pendingErrors = null
                        resetTimer()
                        updateState {
                            copy(progressState = null)
                        }
                    }

                    else -> {
                        this.pendingErrors = listOf(progress.error)
                        set(progress)
                    }
                }
            }

            is SyncProgress.finished -> {
                val errors = progress.errors
                if (errors.isEmpty()) {
                    this.pendingErrors = null
                    resetTimer()
                    set(progress)
                    hideToolbarWithDelay()

                    return
                }

                this.pendingErrors = errors

                set(progress)
                hideToolbarWithDelay()
            }

            SyncProgress.starting -> {
                hideToolbarWithDelay()
            }
            is SyncProgress.shouldMuteWhileOnScreen -> {
                muteProgressDialogsOnSpecificScreens = progress.shouldMute
                //immediately dismiss the progress snackbar.
                if (progress.shouldMute) {
                    this.pendingErrors = null
                    resetTimer()
                    updateState {
                        copy(progressState = null)
                    }
                }
            }

            else -> {
                set(progress)
            }
        }

    }

    private fun set(syncProgress: SyncProgress) {
        if (muteProgressDialogsOnSpecificScreens) {
            return
        }
        val message = syncToolbarTextGenerator.syncToolbarText(syncProgress)
        when (syncProgress) {
            is SyncProgress.aborted -> {
                updateState {
                    copy(
                        progressState = CurrentSyncProgressState.Aborted(
                            message = message,
                        )
                    )
                }
            }

            is SyncProgress.finished -> {
                //Only show sync finished message when there are errors.
                if(!this@SyncToolbarViewModel.pendingErrors.isNullOrEmpty()) {
                    updateState {
                        copy(
                            progressState = CurrentSyncProgressState.SyncFinishedWithError(
                                message = message,
                            )
                        )
                    }
                }
            }
            else -> {
              // We don't show regular progress updates now
            }
        }
    }

    fun showErrorDialog() {
        val errors = this.pendingErrors ?: return
        showErrorAlert(errors)
    }

    private fun showErrorAlert(errors: List<Exception>) {
        updateState {
            copy(progressState = null)
        }
        val error = errors.firstOrNull() ?: return
        val message = syncToolbarTextGenerator.syncToolbarAlertMessage(error)
        updateState {
            copy(dialogErrorMessage = message.first)
        }
    }

    private fun resetTimer() {
        timer?.cancel()
        timer = Timer()
    }

    private fun hideToolbarWithDelay() {
        resetTimer()
        timer?.schedule(timerTask {
            viewModelScope.launch {
                updateState {
                    copy(progressState = null)
                }
            }
        }, finishVisibilityTime)
    }

    fun onDismissDialog() {
        this.pendingErrors = null
        updateState {
            copy(
                dialogErrorMessage = null,
            )
        }
    }

    fun onDismissProgressDialog() {
        updateState {
            copy(progressState = null)
        }
    }
}

data class SyncToolbarViewState(
    val dialogErrorMessage: String? = null,
    val progressState: CurrentSyncProgressState? = null,
) : ViewState

sealed class SyncToolbarViewEffect : ViewEffect {
    object NavigateBack : SyncToolbarViewEffect()
}