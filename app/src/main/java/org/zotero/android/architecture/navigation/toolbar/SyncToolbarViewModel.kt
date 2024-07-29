package org.zotero.android.architecture.navigation.toolbar

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgress
import org.zotero.android.architecture.navigation.toolbar.data.SyncProgressEventStream
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadGroupDbRequest
import org.zotero.android.sync.SyncError
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

@HiltViewModel
class SyncToolbarViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val syncProgressEventStream: SyncProgressEventStream,
) : BaseViewModel2<SyncToolbarViewState, SyncToolbarViewEffect>(SyncToolbarViewState()) {

    private val finishVisibilityTime = 4_000L
    private var timer: Timer? = null

    private var pendingErrors: List<Exception>? = null

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
                            copy(progress = null)
                        }
                    }
                    else -> {
                        this.pendingErrors = listOf(progress.error)
                        set(progress = progress)
                    }
                }
            }
            is SyncProgress.finished -> {
                val errors = progress.errors
                if (errors.isEmpty()) {
                    this.pendingErrors = null
                    resetTimer()
//                    updateState {
//                        copy(progress = null)
//                    }
                    set(progress = progress)
                    hideToolbarWithDelay()

                    return
                }

                this.pendingErrors = errors

                set(progress = progress)
                hideToolbarWithDelay()
            }
            SyncProgress.starting -> {
                hideToolbarWithDelay()
            }
            else -> {
                set(progress = progress)
            }
        }

    }

    private fun set(progress: SyncProgress) {
        updateState {
            copy(progress = progress)
        }

    }

    fun showErrorDialog() {
        val errors = this.pendingErrors ?: return
        showErrorAlert(errors)
    }

    private fun showErrorAlert(errors: List<Exception>) {
        updateState {
            copy(progress = null)
        }
        val error = errors.firstOrNull() ?: return
        updateState {
            copy(dialogError = error)
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
                    copy(progress = null)
                }
            }
        }, finishVisibilityTime)
    }

    fun getGroupNameById(groupId: Int): String {
        val group =
            dbWrapperMain.realmDbStorage.perform(request = ReadGroupDbRequest(identifier = groupId))
        val groupName = group.name ?: "${groupId}"
        return groupName
    }

    fun onDismissDialog() {
        this.pendingErrors = null
        updateState {
            copy(
                dialogError = null,
            )
        }
    }
}

data class SyncToolbarViewState(
    val message: String = "",
    val dialogError: Exception? = null,
    val progress: SyncProgress? = null
) : ViewState

sealed class SyncToolbarViewEffect : ViewEffect {
    object NavigateBack : SyncToolbarViewEffect()
}