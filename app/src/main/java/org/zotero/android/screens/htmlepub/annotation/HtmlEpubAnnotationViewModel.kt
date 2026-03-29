package org.zotero.android.screens.htmlepub.annotation

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
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationArgs
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationColorResult
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationCommentResult
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationDeleteResult
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationScreenClosed
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Tag
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubAnnotationViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<HtmlEpubAnnotationViewState, HtmlEpubAnnotationViewEffect>(HtmlEpubAnnotationViewState()) {

    private var isDeletingAnnotation = false
    private lateinit var args: HtmlEpubAnnotationArgs
    private var pdfReaderThemeCancellable: Job? = null
    private var isTablet: Boolean = false

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.HtmlEpubReaderAnnotationScreen) {
            updateState {
                copy(tags = tagPickerResult.tags.toImmutableList())
            }
            EventBus.getDefault().post(TagPickerResult(tagPickerResult.tags, TagPickerResult.CallPoint.HtmlEpubReaderScreen))
        }
    }


    fun init(args: HtmlEpubAnnotationArgs, isTablet: Boolean) = initOnce {
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
            callPoint = TagPickerResult.CallPoint.HtmlEpubReaderAnnotationScreen,
        )

        triggerEffect(HtmlEpubAnnotationViewEffect.NavigateToTagPickerScreen)
    }

    override fun onCleared() {
        if (!isDeletingAnnotation) {
            postAnnotationCommentResult()
        }
        EventBus.getDefault().post(HtmlEpubAnnotationScreenClosed)
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }

    private fun postAnnotationCommentResult() {
        EventBus.getDefault().post(
            HtmlEpubAnnotationCommentResult(
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
            HtmlEpubAnnotationColorResult(
                annotationKey = viewState.annotation!!.key,
                color = color
            )
        )
    }

    fun onDeleteAnnotation() {
        isDeletingAnnotation = true
        EventBus.getDefault().post(
            HtmlEpubAnnotationDeleteResult(
                key = viewState.annotation!!.key,
            )
        )
        triggerEffect(HtmlEpubAnnotationViewEffect.Back)
    }

    fun onDone() {
        if (!isTablet) {
            postAnnotationCommentResult()
        }
        triggerEffect(HtmlEpubAnnotationViewEffect.Back)
    }

}

internal data class HtmlEpubAnnotationViewState(
    val isDark: Boolean = false,
    val annotation: HtmlEpubAnnotation? = null,
    val tags: ImmutableList<Tag> = persistentListOf(),
    val commentFocusText: String = "",
    val color: String = "",
    val colors: ImmutableList<String> = persistentListOf(),
) : ViewState

internal sealed class HtmlEpubAnnotationViewEffect : ViewEffect {
    object NavigateToTagPickerScreen: HtmlEpubAnnotationViewEffect()
    object Back: HtmlEpubAnnotationViewEffect()
}
