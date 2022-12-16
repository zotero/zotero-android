package org.zotero.android.dashboard

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel @Inject constructor(
) : BaseViewModel2<DashboardViewState, DashboardViewEffect>(DashboardViewState()) {

    fun init(context: Context) = initOnce {
    }

}

internal data class DashboardViewState(
    val snackbarMessage: SnackbarMessage? = null,
) : ViewState

internal sealed class DashboardViewEffect : ViewEffect {
}
