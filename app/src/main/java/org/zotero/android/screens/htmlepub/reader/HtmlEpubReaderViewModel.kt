package org.zotero.android.screens.htmlepub.reader

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pspdfkit.internal.g3.json
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.database.requests.CreateHtmlEpubAnnotationsDbRequest
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.formatter.iso8601WithFractionalSeconds
import org.zotero.android.pdf.data.AnnotationsFilter
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.ARG_HTML_EPUB_READER_SCREEN
import org.zotero.android.screens.htmlepub.reader.data.DocumentUpdate
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderArgs
import org.zotero.android.screens.htmlepub.reader.data.Page
import org.zotero.android.screens.htmlepub.reader.data.ReaderAnnotation
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import org.zotero.android.translator.helper.TranslatorHelper
import org.zotero.android.translator.helper.TranslatorHelper.encodeAsJSONForJavascript
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
    private val dateParser: DateParser,
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
            _select(key = annotation.key, didSelectInDocument = true)
        }
        createDatabaseAnnotations(annotations = annotations)
    }

    private fun _select(key: String?, didSelectInDocument: Boolean) {
        if (key == viewState.selectedAnnotationKey) else {
            return
        }
        val existing = viewState.selectedAnnotationKey
        if (existing != null) {
            add(key = existing)
            if (viewState.selectedAnnotationCommentActive) {
                updateState {
                    copy(selectedAnnotationCommentActive = false)
                }
            }
        }
        if (key == null) {
            updateState {
                copy(selectedAnnotationKey = null)
            }
            return
        }
        updateState {
            copy(selectedAnnotationKey = key)
        }
        if (!didSelectInDocument) {
            updateState {
                copy(focusDocumentKey = key)
            }
        } else {
            updateState {
                copy(focusSidebarKey = key)
            }
        }
        add(key =  key)
    }

    fun add(key: String) {
        if (this.annotations.any { it.key == key }) {
            val updatedAnnotationKeys = viewState.updatedAnnotationKeys?.toMutableList() ?: mutableListOf()
            updatedAnnotationKeys.add(key)
            updateState {
                copy(updatedAnnotationKeys = updatedAnnotationKeys)
            }
        }
    }

    private fun setTags(tags: List<Tag>, key: String) {
        val request =
            EditTagsForItemDbRequest(key = key, libraryId = viewState.library.identifier, tags = tags)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "HtmlEpubReaderViewModel: can't set tags $key")
                updateState {
                    copy(error = Error.cantUpdateAnnotation)
                }
                return@launch
            }
        }
    }

    private fun setComment(comment: String, key: String) {
        val htmlComment = comment //TODO convert to HTML
        val mutableComments = viewState.comments.toMutableMap()
        mutableComments[key] = comment
        updateState {
            copy(comments = mutableComments)
        }

        val values = mapOf(KeyBaseKeyPair(key = FieldKeys.Item.Annotation.comment, baseKey = null) to htmlComment)
        val request = EditItemFieldsDbRequest(key = key, libraryId = viewState.library.identifier, fieldValues = values, dateParser = this.dateParser)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "HtmlEpubReaderViewModel: can't set comment $key")
                updateState {
                    copy(error = Error.cantUpdateAnnotation)
                }
                return@launch
            }
        }
    }

    fun setCommentActive(isActive: Boolean) {
        updateState {
            copy(selectedAnnotationCommentActive = isActive)
        }
    }

    fun deselectSelectedAnnotation() {
        _select(key = null, didSelectInDocument = false)
    }

    fun parseAndCacheIfNeededAttributedComment(annotation: ReaderAnnotation): String? {
        val comment = annotation.comment
        if (comment.isEmpty()) {
            return null
        }
        val attributedComment = viewState.comments[annotation.key]
        if (attributedComment != null) {
            return attributedComment
        }

        parseAndCacheComment(key = annotation.key, comment = comment)
        return viewState.comments[annotation.key]
    }

    private fun parseAndCacheComment(key: String, comment: String) {
        val mutable = viewState.comments.toMutableMap()
        mutable[key] = comment
        updateState {
            copy(comments = mutable)
        }
    }
    private fun toggle(tool: AnnotationTool) {
        if (viewState.activeTool == tool) {
            updateState {
                copy(activeTool = null)
            }
        } else {
            updateState {
                copy(activeTool = tool)
            }
        }
    }

    private fun filterAnnotations(term: String?, filter: AnnotationsFilter?) {
        if (term == null && filter == null) {
            val snapshot = viewState.snapshotKeys ?: return
            updateState {
                copy(
                    snapshotKeys = null,
                    annotationSearchTerm = null,
                    annotationFilter = null,
                    sortedKeys = snapshot,
                )
            }
            return
        }

        val snapshot = viewState.snapshotKeys ?: viewState.sortedKeys
        val filteredKeys = filteredKeys(snapshot, term = term, filter = filter)

        if (viewState.snapshotKeys == null) {
            updateState {
                copy(snapshotKeys = viewState.sortedKeys)
            }
        }
        updateState {
            copy(
                sortedKeys = filteredKeys,
                annotationSearchTerm = term,
                annotationFilter = filter
            )
        }

    }

    private fun filteredKeys(snapshot: List<String>, term: String?, filter: AnnotationsFilter?): List<String> {
        if (term == null && filter == null) {
            return snapshot
        }
        return snapshot.filter{ key ->
                val annotation = this.annotations[key] ?:return@filter false
            filter(annotation = annotation, term = term) && filter(annotation = annotation,  filter)
        }
    }

    private fun filter(annotation: HtmlEpubAnnotation, term: String?): Boolean {
        if (term == null) {
            return true
        }
        return annotation.key.lowercase() == term.lowercase() ||
                annotation.author.contains(term, ignoreCase = true) ||
                annotation.comment.contains(term, ignoreCase = true) ||
                (annotation.text ?: "").contains(term, ignoreCase = true) ||
                annotation.tags.any { it.name.contains(term, ignoreCase = true) }
    }

    private fun filter(annotation: HtmlEpubAnnotation, filter: AnnotationsFilter?): Boolean {
        if (filter == null) {
            return true
        }
        val hasTag =
            if (filter.tags.isEmpty()) true else annotation.tags.any { filter.tags.contains(it.name) }
        val hasColor =
            if (filter.colors.isEmpty()) true else filter.colors.contains(annotation.color)
        return hasTag && hasColor
    }

    fun canUpdate(key: String, item: RItem) : Boolean {
        when(item.changeType) {
            UpdatableChangeType.sync.name -> {
                return true
            }
            UpdatableChangeType.syncResponse.name -> {
                return false
            }
            UpdatableChangeType.user.name -> {
                //no-op
            }
        }
        if (!viewState.selectedAnnotationCommentActive || viewState.selectedAnnotationKey != key) {
            return true
        }

        // Check whether the comment actually changed.
        val newComment = item.fields.where().key(FieldKeys.Item.Annotation.comment).findFirst()?.value
        val oldComment = this.annotations[key]?.comment
        return oldComment == newComment
    }

    fun loadItemAnnotationsAndPage(): Triple<RItem, RealmResults<RItem>, String>? {
        try {
            val itemRequest =
                ReadItemDbRequest(libraryId = viewState.library.identifier, key = viewState.key)
            val item = dbWrapperMain.realmDbStorage.perform(request = itemRequest)
            val pageIndexRequest = ReadDocumentDataDbRequest(
                attachmentKey = viewState.key,
                libraryId = viewState.library.identifier
            )
            val pageIndex = dbWrapperMain.realmDbStorage.perform(request = pageIndexRequest)
            val annotationsRequest = ReadAnnotationsDbRequest(
                attachmentKey = viewState.key,
                libraryId = viewState.library.identifier
            )
            val items = dbWrapperMain.realmDbStorage.perform(request = annotationsRequest)
            return Triple(item, items, pageIndex)
        } catch (error: Exception) {
            Timber.e("HtmlEpubReaderViewModel: can't load annotations")
            return null
        }
    }

    fun loadTypeAndPage(file: File, rawPage: String): Pair<String, Page?> {
        when (this.documentFile.extension.lowercase()) {
            "epub" -> {
                val cfi = if(rawPage.isEmpty()) "_start" else rawPage
                return "epub" to Page.epub(cfi = cfi)
            }
            "html", "htm" -> {
                val scrollYPercent = (rawPage.ifEmpty { "0" }).toDoubleOrNull()
                if (scrollYPercent != null) {
                    return "snapshot" to Page.html(scrollYPercent = scrollYPercent)
                } else {
                    Timber.e("HtmlEpubReaderViewModel: incompatible lastIndexPage stored for ${viewState.key} - $rawPage")
                    return "snapshot" to null
                }
            }
            else -> {
                throw Error.incompatibleDocument
            }
        }
    }
    fun processAnnotations(items: RealmResults<RItem>): Triple<List<String>, Map<String, HtmlEpubAnnotation>, String> {
        val sortedKeys = mutableListOf<String>()
        val annotation = mutableMapOf<String, HtmlEpubAnnotation>()
        val jsons: MutableList<MutableMap<String, Any>> = mutableListOf()
        for (item in items) {
            val (annotation, json) = item.htmlEpubAnnotation ?: continue
            jsons.add(json)
            sortedKeys.add(annotation.key)
            annotations[item.key] = annotation
        }
        val jsonString = TranslatorHelper.encodeAsJSONForJavascript(this.gson, jsons)
        return Triple(sortedKeys, annotations, jsonString)
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
    val activeTool: AnnotationTool? = null,
    val updatedAnnotationKeys: List<String>? = null,
    val selectedAnnotationKey: String? = null,
    val selectedAnnotationCommentActive: Boolean = false,
    val focusSidebarKey: String? = null,
    val focusDocumentKey: String? = null,
    val comments: Map<String, String> = emptyMap(),
    val snapshotKeys: List<String>? = null,
    val sortedKeys: List<String> = emptyList(),
    val annotationSearchTerm: String? = null,
    var annotationFilter: AnnotationsFilter? = null,
) : ViewState {
}

sealed class HtmlEpubReaderViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubReaderViewEffect()
    object DisableForceScreenOn : HtmlEpubReaderViewEffect()
    object EnableForceScreenOn : HtmlEpubReaderViewEffect()
    object ScreenRefresh: HtmlEpubReaderViewEffect()
}