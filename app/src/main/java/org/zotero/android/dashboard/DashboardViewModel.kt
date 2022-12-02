package org.zotero.android.dashboard

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.init.InitUseCase
import org.zotero.android.sync.LibrarySyncType
import org.zotero.android.sync.SyncType
import org.zotero.android.sync.SyncUseCase
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val syncUseCase: SyncUseCase,
    private val initUseCase: InitUseCase,
    private val sdkPrefs: SdkPrefs
) : BaseViewModel2<DashboardViewState, DashboardViewEffect>(DashboardViewState()) {

    fun init(context: Context) = initOnce {
        viewModelScope.launch {
            initUseCase.execute(context)
            syncUseCase.start(userId = sdkPrefs.getUserId(), type = SyncType.normal, libraries = LibrarySyncType.all)
        }
    }

}

internal data class DashboardViewState(
    val snackbarMessage: SnackbarMessage? = null,
) : ViewState

internal sealed class DashboardViewEffect : ViewEffect {
}
