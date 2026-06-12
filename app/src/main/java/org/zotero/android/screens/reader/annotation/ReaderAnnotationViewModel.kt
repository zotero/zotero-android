package org.zotero.android.screens.reader.annotation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationArgs
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationColorResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationCommentResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationDeleteResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationScreenClosed
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Tag
import javax.inject.Inject

@HiltViewModel
internal class ReaderAnnotationViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<ReaderAnnotationViewState, ReaderAnnotationViewEffect>(ReaderAnnotationViewState()) {

    private var isDeletingAnnotation = false
    private lateinit var args: ReaderAnnotationArgs
    private var pdfReaderThemeCancellable: Job? = null
    private var isTablet: Boolean = false

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.ReaderAnnotationScreen) {
            updateState {
                copy(tags = tagPickerResult.tags.toImmutableList())
            }
            EventBus.getDefault().post(TagPickerResult(tagPickerResult.tags, TagPickerResult.CallPoint.ReaderScreen))
        }
    }


    fun init(args: ReaderAnnotationArgs, isTablet: Boolean) = initOnce {
        this.args = args
        this.isTablet = isTablet
        EventBus.getDefault().register(this)
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        val annotation = args.selectedAnnotation!!

        val colors = AnnotationsConfig.colors(annotation.type)
        val selectedColor = annotation.color
        updateState {
            copy(
                color = selectedColor,
                colors = colors.toImmutableList(),
                annotation = annotation,
                tags = args.selectedAnnotation.tags.toImmutableList(),
                commentFocusText = annotation.comment,
            )
        }
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onTagsClicked() {

        val selected = viewState.tags.map { it.name }.toSet()
        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = args.library.identifier,
            selectedTags = selected,
            tags = emptyList(),
            callPoint = TagPickerResult.CallPoint.ReaderAnnotationScreen,
        )

        triggerEffect(ReaderAnnotationViewEffect.NavigateToTagPickerScreen)
    }

    override fun onCleared() {
        if (!isDeletingAnnotation) {
            postAnnotationCommentResult()
        }
        EventBus.getDefault().post(ReaderAnnotationScreenClosed)
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private fun postAnnotationCommentResult() {
        EventBus.getDefault().post(
            ReaderAnnotationCommentResult(
                annotationKey = viewState.annotation!!.key,
                comment = viewState.commentFocusText
            )
        )
    }

    fun onCommentTextChange(comment: String) {
        updateState {
            copy(commentFocusText = comment)
        }
    }

    fun onColorSelected(color: String) {
        updateState {
            copy(color = color)
        }
        EventBus.getDefault().post(
            ReaderAnnotationColorResult(
                annotationKey = viewState.annotation!!.key,
                color = color
            )
        )
    }

    fun onDeleteAnnotation() {
        isDeletingAnnotation = true
        EventBus.getDefault().post(
            ReaderAnnotationDeleteResult(
                key = viewState.annotation!!.key,
            )
        )
        triggerEffect(ReaderAnnotationViewEffect.Back)
    }

    fun onDone() {
        if (!isTablet) {
            postAnnotationCommentResult()
        }
        triggerEffect(ReaderAnnotationViewEffect.Back)
    }

}

internal data class ReaderAnnotationViewState(
    val isDark: Boolean = false,
    val annotation: NewReaderAnnotation? = null,
    val tags: ImmutableList<Tag> = persistentListOf(),
    val commentFocusText: String = "",
    val color: String = "",
    val colors: ImmutableList<String> = persistentListOf(),
) : ViewState

internal sealed class ReaderAnnotationViewEffect : ViewEffect {
    object NavigateToTagPickerScreen: ReaderAnnotationViewEffect()
    object Back: ReaderAnnotationViewEffect()
}
