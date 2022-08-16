package org.zotero.android.sample

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.sample.usecase.SampleUseCase
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val sampleUseCase: SampleUseCase
) : BaseViewModel2<SampleViewState, SampleViewEffect>(SampleViewState()) {


    fun init() = initOnce {
        viewModelScope.launch {
            val result = sampleUseCase.execute().ifFailure {
                return@launch
            }

            updateState { copy(testText = result.samplePayload) }
        }

    }

}

internal data class SampleViewState(
    val testText: String = "initial text",
) : ViewState

internal sealed class SampleViewEffect : ViewEffect {
    object NavigateBack : SampleViewEffect()
}
