package org.zotero.android.screens.htmlepub.reader

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.requests.CreateHtmlEpubAnnotationsDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.formatter.iso8601WithFractionalSeconds
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.ARG_HTML_EPUB_READER_SCREEN
import org.zotero.android.screens.htmlepub.reader.data.DocumentUpdate
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderArgs
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

@HiltViewModel
class HtmlEpubReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val context: Context,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val dispatcher: CoroutineDispatcher,
    private val sessionDataEventStream: SessionDataEventStream,
    private val fileStore: FileStore,
    private val schemaController: SchemaController,
    private val gson: Gson,
    private val dbWrapperMain: DbWrapperMain,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<HtmlEpubReaderViewState, HtmlEpubReaderViewEffect>(HtmlEpubReaderViewState())  {

    private lateinit var originalFile: File
    private lateinit var readerDirectory: File
    private lateinit var documentFile: File
    private lateinit var readerFile: File
    private var userId: Long = 0L
    private var username: String = ""
    private var selectedTextParams: MutableMap<String, Any>? = null
    private var annotations = mutableMapOf<String, HtmlEpubAnnotation>()
    private var documentUpdate: DocumentUpdate? = null

    private var isTablet: Boolean = false

    private var disableForceScreenOnTimer: Timer? = null

    val screenArgs: HtmlEpubReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_HTML_EPUB_READER_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                val isDark = data!!.isDark
                updateState {
                    copy(isDark = isDark)
                }
                triggerEffect(HtmlEpubReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    fun init(
        uri: Uri,
        isTablet: Boolean,
    ) {
        initFileUris(uri)
        restartDisableForceScreenOnTimer()
        this.isTablet = isTablet

        EventBus.getDefault().register(this)

        initState()
        startObservingTheme()

        initialiseReader()

        this.toolColors = mutableMapOf(
            AnnotationTool.HIGHLIGHT to defaults.getHighlightColorHex(),
            AnnotationTool.NOTE to defaults.getNoteColorHex(),
            AnnotationTool.UNDERLINE to defaults.getUnderlineColorHex(),
        )
        this.userId = sessionDataEventStream.currentValue()!!.userId
        this.username = defaults.getUsername()
    }

    private fun initialiseReader() {
        val readerUrl = fileStore.htmlEpubReaderDirectory()
        readerUrl.copyRecursively(target = readerDirectory, overwrite = true)
        originalFile.copyRecursively(target = documentFile, overwrite = true)
    }

    private fun initState() {
        val params = this.screenArgs
        updateState {
            copy(
                key = params.key,
                parentKey = params.parentKey,
                library = params.library,
            )
        }
    }


    private fun initFileUris(uri: Uri) {
        this.originalFile = uri.toFile()
        this.readerDirectory = fileStore.runningHtmlEpubReaderDirectory()
        this.documentFile = fileStore.runningHtmlEpubReaderUserFileSubDirectory()
        this.readerFile = File(readerDirectory, "view.html")
    }

    fun restartDisableForceScreenOnTimer() {
        viewModelScope.launch {
            triggerEffect(HtmlEpubReaderViewEffect.EnableForceScreenOn)
        }
        disableForceScreenOnTimer?.cancel()
        disableForceScreenOnTimer = Timer()
        disableForceScreenOnTimer?.schedule(timerTask {
            viewModelScope.launch {
                triggerEffect(HtmlEpubReaderViewEffect.DisableForceScreenOn)
            }
        }, 25 * 60 * 1000L)
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun onStop() {
        disableForceScreenOnTimer?.cancel()
    }

    private fun parse(annotations: List<Map<String, Any>>, author: String, isAuthor: Boolean): List<HtmlEpubAnnotation> {
        return annotations.mapNotNull { data ->
            val id = data["id"] as? String ?: return@mapNotNull null
            val dateAdded = (data["dateCreated"] as? String?)?.let { iso8601WithFractionalSeconds.parse(it) }
                ?: return@mapNotNull null

            val dateModified = (data["dateModified"] as? String)?.let{ iso8601WithFractionalSeconds.parse(it) }   ?: return@mapNotNull null
            val color = data["color"] as? String ?: return@mapNotNull null
            val comment = data["comment"] as? String ?: return@mapNotNull null
            val pageLabel = data["pageLabel"] as? String   ?: return@mapNotNull null
            val position = data["position"] as? Map<String, Any> ?: return@mapNotNull null
            val sortIndex = data["sortIndex"] as? String ?: return@mapNotNull null
            val text = data["text"] as? String ?: return@mapNotNull null
            val type = (data["type"] as? String)?.let{AnnotationType.valueOf(it)} ?: return@mapNotNull null
            val rawTags = data["tags"] as? List<Map<String, Any>> ?: return@mapNotNull null
            val tags = rawTags.mapNotNull { data ->
                val name = data["name"] as? String ?: return@mapNotNull null
                val color = data["color"] as? String ?: return@mapNotNull null
                Tag(name = name, color = color)
            }
            HtmlEpubAnnotation(
                key = id,
                type = type,
                pageLabel = pageLabel,
                position = position,
                author = author,
                isAuthor = isAuthor,
                color = color,
                comment = comment,
                text = text,
                sortIndex = sortIndex,
                dateAdded = dateAdded,
                dateModified = dateModified,
                tags = tags
            )

        }
    }

    var toolColors: MutableMap<AnnotationTool, String> = mutableMapOf()

    fun params(textParams: Map<String, Any>, type: AnnotationType): Map<String, Any>? {
        val color: String
        when(type) {
            AnnotationType.highlight -> {
                color = toolColors[AnnotationTool.HIGHLIGHT] ?: defaults.getHighlightColorHex()
            }
            AnnotationType.underline -> {
                color = toolColors[AnnotationTool.UNDERLINE] ?: defaults.getUnderlineColorHex()
            }
            AnnotationType.note, AnnotationType.image, AnnotationType.ink, AnnotationType.text -> {
                return null
            }
        }

        val date = Date()
        val params = textParams.toMutableMap()
        params["id"] = KeyGenerator.newKey()
        params["type"] = type.name
        params["color"] = color
        params["dateModified"] = iso8601WithFractionalSeconds.format(date)
        params["dateCreated"] = iso8601WithFractionalSeconds.format(date)
        params["tags"] = emptyList<String>()
        params["pageLabel"] = ""
        params["comment"] = ""
        return params
    }

    private fun createDatabaseAnnotations(annotations: List<HtmlEpubAnnotation>) {
        val request = CreateHtmlEpubAnnotationsDbRequest(
            attachmentKey = viewState.key,
            libraryId = viewState.library.identifier,
            annotations = annotations,
            userId = this.userId,
            schemaController = schemaController,
            gson = gson,
        )
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "HtmlEpubReaderViewModel: could not store annotations")
                updateState {
                    copy(error = Error.cantAddAnnotations)
                }
                return@launch
            }
        }
    }

    sealed class Error : Exception() {
        object cantDeleteAnnotation: Error()
        object cantAddAnnotations: Error()
        object cantUpdateAnnotation: Error()
        object incompatibleDocument: Error()
        object unknown: Error()

        val title: Int get(){
            when (this) {
                cantAddAnnotations, cantDeleteAnnotation, cantUpdateAnnotation, incompatibleDocument, unknown -> {
                    return Strings.error
                }
            }
        }

        val messageS: Int get() {
            when (this) {
                Error.cantDeleteAnnotation -> {
                    return Strings.errors_pdf_cant_delete_annotations
                }

                Error.cantAddAnnotations -> {
                    return Strings.errors_pdf_cant_add_annotations
                }

                cantUpdateAnnotation -> {
                    return Strings.errors_pdf_cant_update_annotation
                }

                incompatibleDocument -> {
                    return Strings.errors_pdf_incompatible_document
                }

                unknown -> {
                    return Strings.errors_unknown
                }
            }
        }
    }

    private fun saveAnnotationFromSelection(type: AnnotationType) {
        val textParams = this.selectedTextParams?.get("annotation") as? Map<String, Any> ?: return
        val params = params(textParams, type = type) ?: return
        val annotations =
            parse(annotations = listOf(params), author = this.username, isAuthor = true)
        this.selectedTextParams = null

        for (annotation in annotations) {
            this.annotations[annotation.key] = annotation
        }
        documentUpdate = DocumentUpdate(
            deletions = emptyList(),
            insertions = listOf(params),
            modifications = emptyList()
        )
        createDatabaseAnnotations(annotations = annotations)
    }

    private fun saveAnnotations(params: Map<String, Any>) {
        val rawAnnotations = params["annotations"] as? List<Map<String, Any>>
        if (rawAnnotations == null || rawAnnotations.isEmpty()) {
            Timber.e("HtmlEpubReaderViewModel: annotations missing or empty - ${params["annotations"] ?: emptyList<String>()}")
            return
        }

        val annotations =
            parse(annotations = rawAnnotations, author = this.username, isAuthor = true)

        if (annotations.isEmpty()) {
            Timber.e("HtmlEpubReaderViewModel: could not parse annotations")
            return
        }

        // Disable annotation tool & select annotation
        val annotation = annotations.firstOrNull {it.type == AnnotationType.note }
        if (annotation != null) {
            updateState {
                copy(activeTool = null)
            }
           //TODO select
        }
        createDatabaseAnnotations(annotations = annotations)
    }


}

data class HtmlEpubReaderViewState(
    val key: String = "",
    val parentKey: String? = null,
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val isDark: Boolean = false,
    val error: HtmlEpubReaderViewModel.Error? = null,
    val activeTool: AnnotationTool? = null
) : ViewState {
}

sealed class HtmlEpubReaderViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubReaderViewEffect()
    object DisableForceScreenOn : HtmlEpubReaderViewEffect()
    object EnableForceScreenOn : HtmlEpubReaderViewEffect()
    object ScreenRefresh: HtmlEpubReaderViewEffect()
}