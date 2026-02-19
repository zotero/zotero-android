package org.zotero.android.screens.htmlepub.reader

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import android.webkit.WebView
import androidx.compose.ui.text.TextStyle
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmObjectChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.database.requests.CreateHtmlEpubAnnotationsDbRequest
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.ReadItemDbRequest
import org.zotero.android.database.requests.StorePageForItemDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.formatter.iso8601DateFormatV3
import org.zotero.android.helpers.formatter.iso8601WithFractionalSeconds
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.ARG_HTML_EPUB_READER_SCREEN
import org.zotero.android.screens.htmlepub.htmlEpubFilter.data.HtmlEpubFilterArgs
import org.zotero.android.screens.htmlepub.reader.data.AnnotationTool
import org.zotero.android.screens.htmlepub.reader.data.DocumentData
import org.zotero.android.screens.htmlepub.reader.data.DocumentUpdate
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotationsFilter
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderArgs
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebData
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderWebError
import org.zotero.android.screens.htmlepub.reader.data.Outline
import org.zotero.android.screens.htmlepub.reader.data.Page
import org.zotero.android.screens.htmlepub.reader.data.ReaderAnnotation
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchResultsData
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchResultsEventStream
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchTermEventStream
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubOutline
import org.zotero.android.screens.htmlepub.reader.sidebar.data.HtmlEpubReaderSliderOptions
import org.zotero.android.screens.htmlepub.reader.web.HtmlEpubReaderWebCallChainExecutor
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import org.zotero.android.translator.helper.TranslatorHelper
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
    private val dispatchers: Dispatchers,
    private val htmlEpubReaderSearchResultsEventStream: HtmlEpubReaderSearchResultsEventStream,
    private val htmlEpubReaderSearchTermEventStream: HtmlEpubReaderSearchTermEventStream,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<HtmlEpubReaderViewState, HtmlEpubReaderViewEffect>(HtmlEpubReaderViewState())  {

    private val mainCoroutineScope = CoroutineScope(dispatchers.main)

    private lateinit var originalFile: File
    private lateinit var readerDirectory: File
    private lateinit var documentFile: File
    private lateinit var readerFile: File
    private var userId: Long = 0L
    private var username: String = ""
    private var selectedTextParams: JsonObject? = null
    private var annotations = mutableMapOf<String, HtmlEpubAnnotation?>()
    private var texts = mutableMapOf<String, Pair<String, Map<TextStyle, String>>?>()
    private val onCommentChangeFlow = MutableStateFlow<Pair<String, String>?>(null)
    private val onOutlineSearchStateFlow = MutableStateFlow("")
    private val onAnnotationSearchStateFlow = MutableStateFlow("")
    private lateinit var textFont: TextStyle

    private var isTablet: Boolean = false

    private var disableForceScreenOnTimer: Timer? = null

    private var item: RItem? = null
    private var annotationItems: RealmResults<RItem>? = null

    private var htmlEpubReaderWebCallChainExecutor: HtmlEpubReaderWebCallChainExecutor? = null

    var toolColors: MutableMap<AnnotationTool, String> = mutableMapOf()

    var outlines: MutableList<HtmlEpubOutline> = mutableListOf()


    val screenArgs: HtmlEpubReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_HTML_EPUB_READER_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private var htmlEpubReaderSearchTermCancellable: Job? = null
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

    private fun startObservingSearchTerm() {
        this.htmlEpubReaderSearchTermCancellable = htmlEpubReaderSearchTermEventStream.flow()
            .drop(1)
            .onEach { data ->
                val string = data?.term ?: ""
                if (string.isEmpty()) {
                    htmlEpubReaderWebCallChainExecutor?.clearSearch()
                    htmlEpubReaderSearchResultsEventStream.emitAsync(HtmlEpubReaderSearchResultsData(null))
                    return@onEach
                }

                htmlEpubReaderWebCallChainExecutor?.search(string)
            }
            .launchIn(viewModelScope)
    }

    fun init(
        isTablet: Boolean,
        textFont: TextStyle,
        webView: WebView
    ) {
        this.textFont = textFont
        val uri = screenArgs.uri
        initFileUris(uri)
        restartDisableForceScreenOnTimer()
        this.isTablet = isTablet

//        EventBus.getDefault().register(this)

        initState()
        startObservingTheme()
        startObservingSearchTerm()

        setupCommentChangeFlow()
        setupAnnotationSearchStateFlow()
        setupOutlineSearchStateFlow()

        setupWebView(webView)

        initialiseReader()

        this.toolColors = mutableMapOf(
            AnnotationTool.highlight to defaults.getHighlightColorHex(),
            AnnotationTool.note to defaults.getNoteColorHex(),
            AnnotationTool.underline to defaults.getUnderlineColorHex(),
        )
        this.userId = sessionDataEventStream.currentValue()!!.userId
        this.username = defaults.getUsername()
    }

    private fun initialiseReader() {
        val readerUrl = fileStore.htmlEpubReaderDirectory()
        readerUrl.copyRecursively(target = readerDirectory, overwrite = true)
        originalFile.copyRecursively(target = documentFile, overwrite = true)

        htmlEpubReaderWebCallChainExecutor?.start(this.readerFile)

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
        this.documentFile = fileStore.runningHtmlEpubReaderUserFileSubDirectory(originalFile.extension)
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

    private fun setTopBarVisibility(isVisible: Boolean) {
        updateState {
            copy(isTopBarVisible = isVisible)
        }
    }

    private fun loadOutlines() {
        createSnapshot("")
        updateState { copy(isOutlineEmpty = this@HtmlEpubReaderViewModel.outlines.isEmpty()) }
    }

    private fun createSnapshot(search: String) {
        val snapshot = mutableListOf<HtmlEpubOutline>()
        append(outlines = this.outlines, parent = null, snapshot = snapshot, search = search)
        if (snapshot.size == 1) {
            updateState {
                copy(
                    outlineSnapshot = snapshot,
                    outlineExpandedNodes = setOf(snapshot[0].id)
                )
            }
        } else {
            updateState {
                copy(
                    outlineSnapshot = snapshot,
                    outlineExpandedNodes = emptySet()
                )
            }
        }
    }

    private fun append(
        outlines: List<HtmlEpubOutline>,
        parent: HtmlEpubOutline?,
        snapshot: MutableList<HtmlEpubOutline>,
        search: String
    ) {
        val rows = mutableListOf<HtmlEpubOutline>()
        for (element in outlines) {
            if (search.isEmpty()) {
                val outline = HtmlEpubOutline(
                    id = element.id,
                    title = element.title,
                    location = element.location,
                    children = mutableListOf(),
                    isActive = true,
                )
                rows.add(outline)
                continue
            }

            val elementContainsSearch = outline(element, search)
            val childContainsSearch = child(element.children, search)

            if (!elementContainsSearch && !childContainsSearch) {
                continue
            }

            val outline = element.copy(isActive = elementContainsSearch)
            rows.add(outline)
        }
        if (parent == null) {
            snapshot.addAll(rows)
        } else {
            parent.children.addAll(rows)
        }

        for ((idx, element) in outlines.withIndex()) {
            val children = element.children

            if (search.isEmpty()) {
                append(
                    outlines = children,
                    parent = rows[idx],
                    snapshot = snapshot,
                    search = search
                )
                continue
            }

            val index = rows.indexOfFirst { row ->
                row.title == element.title

            }
            if (index == -1) {
                continue
            }

            append(outlines = children, parent = rows[index], snapshot = snapshot, search = search)
        }
    }

    private fun child(children: List<HtmlEpubOutline>, string: String): Boolean {
        if (children.isEmpty()) {
            return false
        }

        for (child in children) {
            if (outline(child, string)) {
                return true
            }

            val children = child.children

            if (children != null && child(children, string)) {
                return true
            }
        }

        return false
    }

    private fun outline(outline: HtmlEpubOutline, string: String): Boolean {
        return (outline.title).contains(string, ignoreCase = true)
    }

    private fun parseOutline(data: JsonObject) {
        val params = data["params"]?.asJsonObject ?: return
        val outline = params["outline"]?.asJsonArray ?: return
        val outlines = mutableListOf<Outline>()
        for (item in outline) {
            val outline = parseOutline2(item.asJsonObject) ?: continue
            outlines.add(outline)
        }
        this.outlines = outlines.map { HtmlEpubOutline(it, isActive = true) }.toMutableList()
    }

    fun parseOutline2(data: JsonObject): Outline? {
        val title = data["title"]?.asString ?: return null
        val location = data["location"]?.asJsonObject ?: return null
        val rawChildren = data["items"]?.asJsonArray ?: return null
        val children = rawChildren.mapNotNull { parseOutline2(it.asJsonObject) }
        return Outline(
            title = title.trim().trim { it == '\n' },
            location = location.asMap(),
            children = children,
            isActive = true,
        )
    }

    private fun parse(annotations: JsonArray, author: String, isAuthor: Boolean): List<HtmlEpubAnnotation> {
        return annotations.mapNotNull { dataAsJson ->
            val data = dataAsJson.asJsonObject
            val id = data["id"]?.asString ?: return@mapNotNull null
            val dateAdded = (data["dateCreated"]?.asString)?.let {
                println()
                iso8601WithFractionalSeconds.parse(it)
            }
                ?: return@mapNotNull null

            val dateModified = (data["dateModified"]?.asString)?.let{ iso8601WithFractionalSeconds.parse(it) }   ?: return@mapNotNull null
            val color = data["color"]?.asString ?: return@mapNotNull null
            val comment = data["comment"]?.asString ?: return@mapNotNull null
            val pageLabel = data["pageLabel"]?.asString   ?: return@mapNotNull null
            val position = data["position"]?.asJsonObject ?: return@mapNotNull null
            val sortIndex = data["sortIndex"]?.asString ?: return@mapNotNull null
            val text = data["text"]?.asString ?: return@mapNotNull null
            val type = (data["type"]?.asString)?.let{AnnotationType.valueOf(it)} ?: return@mapNotNull null
            val rawTags = data["tags"]?.asJsonArray ?: return@mapNotNull null
            val tags = rawTags.mapNotNull { dataAsJson ->
                val data = dataAsJson.asJsonObject
                val name = data["name"]?.asString ?: return@mapNotNull null
                val color = data["color"]?.asString ?: return@mapNotNull null
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

    fun params(textParams: JsonObject, type: AnnotationType): JsonObject? {
        val color: String
        when(type) {
            AnnotationType.highlight -> {
                color = toolColors[AnnotationTool.highlight] ?: defaults.getHighlightColorHex()
            }
            AnnotationType.underline -> {
                color = toolColors[AnnotationTool.underline] ?: defaults.getUnderlineColorHex()
            }
            AnnotationType.note, AnnotationType.image, AnnotationType.ink, AnnotationType.text -> {
                return null
            }
        }

        val date = Date()
        val params = textParams
        params.addProperty("id",KeyGenerator.newKey())
        params.addProperty("type", type.name)
        params.addProperty("color", color)
        params.addProperty("dateModified", iso8601DateFormatV3.format(date))
        params.addProperty("dateCreated", iso8601DateFormatV3.format(date))
        params.add("tags", JsonArray())
        params.addProperty("pageLabel", "")
        params.addProperty("comment", "")
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

    private suspend fun saveAnnotationFromSelection(type: AnnotationType) {
        val textParams = this.selectedTextParams?.get("annotation")?.asJsonObject ?: return
        val params = params(textParams, type = type) ?: return

        val array = JsonArray()
        array.add(params)

        val annotations =
            parse(annotations = array, author = this.username, isAuthor = true)
        this.selectedTextParams = null

        for (annotation in annotations) {
            this.annotations[annotation.key] = annotation
        }

        val documentUpdate = DocumentUpdate(
            deletions = JsonArray(),
            insertions = array,
            modifications = JsonArray()
        )
        htmlEpubReaderWebCallChainExecutor?.updateView(
            modifications = documentUpdate.modifications,
            insertions = documentUpdate.insertions,
            deletions = documentUpdate.deletions
        )


        createDatabaseAnnotations(annotations = annotations)
    }

    private fun saveAnnotations(params: JsonObject) {
        val rawAnnotations = params["annotations"]?.asJsonArray
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

    fun selectAnnotationFromDocument(key: String) {
        _select(key = key, didSelectInDocument = true)
    }


    private fun _select(key: String?, didSelectInDocument: Boolean) {
        if (key == viewState.selectedAnnotationKey) {
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
    fun toggle(toolSet: AnnotationTool) {
        if (viewState.activeTool == toolSet) {
            updateState {
                copy(activeTool = null)
            }
        } else {
            updateState {
                copy(activeTool = toolSet)
            }
        }
        val tool = viewState.activeTool
        val color = tool?.let { this.toolColors[it] }

        if (tool != null && color != null) {
            setTool(tool to color)
        } else {
            setTool(null)
        }
    }

    private fun setTool(data: Pair<AnnotationTool, String>?) {
        viewModelScope.launch {
            if (data == null) {
                htmlEpubReaderWebCallChainExecutor?.clearTool()
                return@launch
            }
            val (tool, color) = data

            var toolName: String
            when(tool) {
                AnnotationTool.highlight -> {
                    toolName = "highlight"
                }
                AnnotationTool.note -> {
                    toolName = "note"
                }
                AnnotationTool.underline -> {
                    toolName = "underline"
                }
            }
            htmlEpubReaderWebCallChainExecutor?.setTool(toolName, color)

        }
    }


    private fun filterAnnotations(term: String, filter: HtmlEpubAnnotationsFilter?) {
        if (term.isEmpty() && filter == null) {
            val snapshot = viewState.snapshotKeys ?: return
            updateState {
                copy(
                    snapshotKeys = null,
                    annotationSearchTerm = "",
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

    private fun filteredKeys(snapshot: List<String>, term: String?, filter: HtmlEpubAnnotationsFilter?): List<String> {
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

    private fun filter(annotation: HtmlEpubAnnotation, filter: HtmlEpubAnnotationsFilter?): Boolean {
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
    fun processAnnotations(items: RealmResults<RItem>): Triple<List<String>, Map<String, HtmlEpubAnnotation?>, String> {
        val sortedKeys = mutableListOf<String>()
        val annotations = mutableMapOf<String, HtmlEpubAnnotation>()
        val jsons = JsonArray()
        for (item in items) {
            val (annotation, json) = item.htmlEpubAnnotation ?: continue
            jsons.add(json)
            sortedKeys.add(annotation.key)
            annotations[item.key] = annotation
        }
        val jsonString = TranslatorHelper.encodeAsJSONForJavascript(this.gson, jsons)
        return Triple(sortedKeys, annotations, jsonString)
    }

    fun update(
        changeSet: OrderedCollectionChangeSet,
        objects: RealmResults<RItem>
    ) {
        val frozenObjects = objects.freeze()
        val deletions = changeSet.deletions
        var insertions = changeSet.insertions
        val modifications = changeSet.changes

        if (deletions.isEmpty() && insertions.isEmpty() && modifications.isEmpty()) {
            insertions = IntArray(frozenObjects.size) { it }
        }
        Timber.i("HtmlEpubReaderViewModel: annotations changed in database")
        var keys = (viewState.snapshotKeys ?: viewState.sortedKeys).toMutableList()
        var annotations = this.annotations
        var texts = this.texts
        var comments = viewState.comments.toMutableMap()
        var selectionDeleted = false
        var popoverWasInserted = false

        val updatedKeys = mutableListOf<String>()
        val updatedPdfAnnotations = JsonArray()
        val deletedPdfAnnotations = JsonArray()
        val insertedPdfAnnotations = JsonArray()

        for (index in modifications) {
            if (index >= keys.size) {
                Timber.w(
                    "HtmlEpubReaderViewModel: tried modifying index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications"
                )
                continue
            }

            val key = keys[index]
            val item = objects.where().key(key).findFirst() ?: continue
            val (annotation, json) = item.htmlEpubAnnotation ?: continue


            Timber.w("HtmlEpubReaderViewModel: update Html/Epub annotation $key")
            annotations[key] = annotation
            updatedPdfAnnotations.add(json)

            if (canUpdate(key = key, item = item)) {
                Timber.i("HtmlEpubReaderViewModel: update sidebar key $key")
                updatedKeys.add(key)

                if (item.changeType == UpdatableChangeType.sync.name) {
                    // Update comment if it's remote sync change
                    Timber.i("HtmlEpubReaderViewModel: update comment")
                    var textCacheTuple: Pair<String, Map<TextStyle, String>>?
                    val comment: String?
                    when (annotation.type) {
                        AnnotationType.highlight, AnnotationType.underline -> {
                            textCacheTuple = annotation.text?.let {
                                it to mapOf(this.textFont to it)
                            }
                        }
                        AnnotationType.note, AnnotationType.image, AnnotationType.ink, AnnotationType.text -> {
                            textCacheTuple = null
                        }

                    }
                    texts[key] = textCacheTuple
                    when (annotation.type) {
                        AnnotationType.note, AnnotationType.highlight, AnnotationType.image, AnnotationType.underline -> {
                            comment = annotation.comment //TODO comment attribute conversion
                        }
                        AnnotationType.ink, AnnotationType.text -> {
                            comment = null
                        }
                    }
                    comments[key] = comment
                }
            }
        }

        var shouldCancelUpdate = false

        for (index in deletions.reversed()) {
            if (index >= keys.size) {
                Timber.w(
                    "HtmlEpubReaderViewModel: tried removing index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications"
                )
                shouldCancelUpdate = true
                break
            }

            val key = keys.removeAt(index)
            annotations[key] = null
            deletedPdfAnnotations.add(key)
            Timber.i("HtmlEpubReaderViewModel: delete key $key")

            if (viewState.selectedAnnotationKey == key) {
                Timber.i("HtmlEpubReaderViewModel: deleted selected annotation")
                selectionDeleted = true
            }
        }

        if (shouldCancelUpdate) {
            return
        }

        for (index in insertions) {
            if (index > keys.size) {
                Timber.w("HtmlEpubReaderViewModel: tried inserting index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                shouldCancelUpdate = true
                break
            }

            val item = objects[index]!!

            val htmlEpubAnnotation = item.htmlEpubAnnotation
            if (htmlEpubAnnotation == null) {
                Timber.w("HtmlEpubReaderViewModel: tried adding invalid annotation")
                shouldCancelUpdate = true
                break
            }
            val (annotation, json) = htmlEpubAnnotation

            keys.add(index, item.key)
            annotations[item.key] = annotation
            if (viewState.annotationPopoverKey == item.key) {
                popoverWasInserted = true
            }
            Timber.i("HtmlEpubReaderViewModel: insert key ${item.key}")
            when (item.changeType) {
                UpdatableChangeType.sync.name, UpdatableChangeType.syncResponse.name -> {
                    insertedPdfAnnotations.add(json)
                    Timber.i("HtmlEpubReaderViewModel: insert Html/Epub annotation")
                }
            }
        }
        if (shouldCancelUpdate) {
            return
        }

        updateState {
            if (viewState.snapshotKeys == null) {
                copy(sortedKeys = keys)
            } else {
                copy(snapshotKeys = keys)
                copy(sortedKeys = filteredKeys(
                    snapshot = keys,
                    term = viewState.annotationSearchTerm,
                    filter = viewState.annotationFilter
                ))
            }
            copy(comments = comments)
        }
        this.annotations = annotations
        val documentUpdate = DocumentUpdate(
            deletions = deletedPdfAnnotations,
            insertions = insertedPdfAnnotations,
            modifications = updatedPdfAnnotations
        )

        viewModelScope.launch {
            htmlEpubReaderWebCallChainExecutor?.updateView(
                modifications = documentUpdate.modifications,
                insertions = documentUpdate.insertions,
                deletions = documentUpdate.deletions
            )
        }

        this.texts = texts
        updateState {
            copy(updatedAnnotationKeys = updatedKeys.filter { viewState.sortedKeys.contains(it) })
        }
        if (popoverWasInserted) {
            //TODO show popover
        }
        if (selectionDeleted) {
            _select(key = null, didSelectInDocument = true)
            updateState {
                copy(
                    annotationPopoverKey = null,
                    annotationPopoverRect = null
                )
            }
            //TODO show popover
        }
        if ((viewState.snapshotKeys ?: viewState.sortedKeys).isEmpty()) {
            updateState {
                copy(sidebarEditingEnabled = false)
            }
        }
    }

    private fun startObservingItem() {
        this.item?.addChangeListener(RealmObjectChangeListener<RItem> { item, changeSet ->
            if (changeSet?.changedFields?.contains("fields") == true && !changeSet.isDeleted) {
                checkWhetherMd5Changed(item)
            }
        })
    }

    private fun startObservingAnnotationResults() {
        this.annotationItems?.addChangeListener { items, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    update(changeSet, items)

                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    update(changeSet, items)
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "HtmlEpubReaderViewModel: could not load results")
                }

                else -> {
                    //no-op
                }
            }
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)

        item?.removeAllChangeListeners()
        annotationItems?.removeAllChangeListeners()

        deinitialiseReader()
        super.onCleared()
    }

    private fun deinitialiseReader() {
        this.readerDirectory.deleteRecursively()
    }

    fun setupWebView(webView: WebView) {
        this.htmlEpubReaderWebCallChainExecutor = HtmlEpubReaderWebCallChainExecutor(
            context = context,
            dispatchers = dispatchers,
            gson = gson,
            fileStore = fileStore,
            webView = webView,
        )
        this.htmlEpubReaderWebCallChainExecutor?.observable?.flow()
            ?.onEach { result ->
                process(result)
            }
            ?.launchIn(mainCoroutineScope)
    }

    private fun process(result: Result<HtmlEpubReaderWebData>) {
        if (result is Result.Failure) {
            val customException = (result.exception as? HtmlEpubReaderWebError)?: return
            val errorMessage = when (customException) {
                HtmlEpubReaderWebError.failedToInitializeWebView -> {
                    Timber.e("HtmlEpubReaderViewModel: HtmlEpub Worker's JS failed to initialize")
                    context.getString(Strings.retrieve_metadata_error_failed_to_initialize)
                }
            }
            //TODO process error
            return
        }
        val successValue = (result as Result.Success).value
        when (successValue) {
            HtmlEpubReaderWebData.loadDocument -> {
                load()
            }
            is HtmlEpubReaderWebData.parseOutline -> {
                parseOutline(successValue.params)
                loadOutlines()
            }
            HtmlEpubReaderWebData.deselectSelectedAnnotation -> {
                deselectSelectedAnnotation()
            }
            is HtmlEpubReaderWebData.processDocumentSearchResults -> {
                htmlEpubReaderSearchResultsEventStream.emitAsync(
                    HtmlEpubReaderSearchResultsData(
                        successValue.params
                    )
                )
            }
            is HtmlEpubReaderWebData.saveAnnotations -> {
                saveAnnotations(successValue.params)
            }
            is HtmlEpubReaderWebData.selectAnnotationFromDocument -> {
                selectAnnotationFromDocument(successValue.key)
            }
            is HtmlEpubReaderWebData.setSelectedTextParams -> {
                setSelectedTextParams(successValue.params)
            }
            is HtmlEpubReaderWebData.setViewState -> {
                setViewState(successValue.params)
            }
            is HtmlEpubReaderWebData.showUrl -> TODO()
            HtmlEpubReaderWebData.toggleInterfaceVisibility -> TODO()
        }
    }



    fun setViewState(params: JsonObject) {
        val state = params["state"]?.asJsonObject
        if (state == null) {
            Timber.e("HtmlEpubReaderViewModel: invalid params - $params")
            return
        }

        val page: String
        val scrollPercent = state["scrollYPercent"]
        if (scrollPercent != null && scrollPercent.isJsonPrimitive) {
            page = "${scrollPercent.asDouble.rounded(1)}"
        } else if (state["cfi"]?.asString != null) {
            page = state["cfi"].asString
        } else {
            return
        }

        val request = StorePageForItemDbRequest(key = viewState.key, libraryId = viewState.library.identifier, page = page)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "HtmlEpubReaderViewModel: can't store page")
                return@launch
            }
        }

    }

    private fun setSelectedTextParams(params: JsonObject) {
        this.selectedTextParams = params
    }

    fun load() {
        try {
            val (item, annotationItems, rawPage) = loadItemAnnotationsAndPage() ?: return

            if (checkWhetherMd5Changed(item)) {
                return
            }

            val (sortedKeys, annotations, json) = processAnnotations(items = annotationItems)
            val (type, page) = loadTypeAndPage(this.documentFile, rawPage = rawPage)
            val documentData = DocumentData(
                type = type,
                file = this.documentFile,
                annotationsJson = json,
                page = page,
                selectedAnnotationKey = viewState.selectedAnnotationKey
            )

            this.item = item
            startObservingItem()
            this.annotationItems = annotationItems
            startObservingAnnotationResults()

            this.annotations = annotations.toMutableMap()
            updateState {
                copy(sortedKeys = sortedKeys)
            }
            viewModelScope.launch {
                htmlEpubReaderWebCallChainExecutor?.loadDocument(documentData)
            }
        } catch (e: Exception) {
            Timber.e(e, "HtmlEpubReaderViewModel: could not load document")
        }
    }


    fun checkWhetherMd5Changed(item: RItem): Boolean {
        val md5 = FileHelper.cachedMD5(this.originalFile)
        if (md5 == null || item.backendMd5.isEmpty() || item.backendMd5 == md5) {
            return false
        }
        //TODO show document changed alert
        return true
    }

    fun hidePdfSearch() {
        updateState {
            copy(
                showPdfSearch = false
            )
        }
    }
    fun togglePdfSearch() {
        updateState {
            copy(showPdfSearch = !showPdfSearch)
        }

    }

    fun toggleSideBar() {
        val newShowSideBarState = !viewState.showSideBar
        updateState {
            copy(showSideBar = newShowSideBarState)
        }
        val selectedAnnotationKey = viewState.selectedAnnotationKey
        if (newShowSideBarState && selectedAnnotationKey != null) {
            val index = viewState.sortedKeys.indexOf(selectedAnnotationKey)
            triggerEffect(
                HtmlEpubReaderViewEffect.ScrollSideBar(index)
            )
        }
    }

    fun toggleToolbarButton() {
        updateState {
            copy(showCreationToolbar = !viewState.showCreationToolbar)
        }
        val tool = viewState.activeTool ?: return
        toggle(tool)
    }

    fun navigateToPdfSettings() {
       //TODO
    }


    fun annotation(key: String): HtmlEpubAnnotation? {
        return this.annotations[key]
    }

    fun onCommentFocusFieldChange(annotationKey: String) {
        val annotation =
            annotation(annotationKey)
                ?: return
        selectAnnotationFromDocument(key = annotationKey)

        updateState {
            copy(
                commentFocusKey = annotationKey,
                commentFocusText = annotation.comment
            )
        }
    }

    fun onCommentTextChange(annotationKey: String, comment: String) {
        updateState {
            copy(commentFocusText = comment)
        }
        onCommentChangeFlow.tryEmit(annotationKey to comment)
    }

    private fun setupCommentChangeFlow() {
        onCommentChangeFlow
            .debounce(500)
            .map { data ->
                if (data != null) {
                    setComment(data.first, data.second)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun searchAnnotations(term: String) {
        val trimmedTerm = term.trim().trim { it == '\n' }
        filterAnnotations(term = trimmedTerm, filter = viewState.filter)
    }

    fun onAnnotationSearch(text: String) {
        updateState {
            copy(annotationSearchTerm = text)
        }
        onAnnotationSearchStateFlow.tryEmit(text)
    }

    private fun setupAnnotationSearchStateFlow() {
        onAnnotationSearchStateFlow
            .debounce(150)
            .map { text ->
                searchAnnotations(text)
            }
            .launchIn(viewModelScope)
    }

    fun onMoreOptionsForItemClicked() {
        //TODO
    }

    fun onTagsClicked(annotation: HtmlEpubAnnotation) {
        //TODO
    }

    fun selectAnnotation(key: String) {
        if (!viewState.sidebarEditingEnabled && key != viewState.selectedAnnotationKey) {
            _select(key = key, didSelectInDocument = false)
        }
    }

    fun setSidebarSliderSelectedOption(optionOrdinal: Int) {
        val option = HtmlEpubReaderSliderOptions.entries[optionOrdinal]
        updateState {
            copy(sidebarSliderSelectedOption = option)
        }
    }

    fun showFilterPopup() {
        val colors = mutableSetOf<String>()
        val tags = mutableSetOf<Tag>()

        val processAnnotation: (HtmlEpubAnnotation) -> Unit = { annotation ->
            colors.add(annotation.color)
            for (tag in annotation.tags) {
                tags.add(tag)
            }
        }

        for (annotation in this.annotations.values) {
            processAnnotation(annotation!!)
        }

        val sortedTags = tags.sortedWith { lTag, rTag ->
            if (lTag.color.isEmpty() == rTag.color.isEmpty()) {
                return@sortedWith lTag.name.compareTo(other = rTag.name, ignoreCase = true)
            }
            if (!lTag.color.isEmpty() && rTag.color.isEmpty()) {
                return@sortedWith 1
            }
            -1
        }

        val sortedColors = mutableListOf<String>()
        AnnotationsConfig.allColors.forEach { color ->
            if (colors.contains(color)) {
                sortedColors.add(color)
            }
        }
        ScreenArguments.htmlEpubFilterArgs = HtmlEpubFilterArgs(
            filter = viewState.filter,
            availableColors = sortedColors,
            availableTags = sortedTags
        )
        triggerEffect(HtmlEpubReaderViewEffect.ShowPdfFilters)
    }

    fun onOutlineSearch(search: String) {
        if (search == viewState.outlineSearchTerm) {
            return
        }
        updateState {
            copy(outlineSearchTerm = search)
        }
        onOutlineSearchStateFlow.tryEmit(search)
    }

    private fun searchOutlines(search: String) {
        createSnapshot(search = search)
    }

    private fun setupOutlineSearchStateFlow() {
        onOutlineSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                searchOutlines(text)
            }
            .launchIn(viewModelScope)
    }

    fun onOutlineItemTapped(outline: HtmlEpubOutline) {
        viewModelScope.launch {
            if (!outline.isActive) {
                return@launch
            }
            htmlEpubReaderWebCallChainExecutor?.show(location = outline.location)
            if (!isTablet) {
                toggleSideBar()
            }
        }

    }

    fun onOutlineItemChevronTapped(outline: HtmlEpubOutline) {
        val expandedNodes = viewState.outlineExpandedNodes
        val newState = if (expandedNodes.contains(outline.id)) {
            expandedNodes - outline.id
        } else {
            expandedNodes + outline.id
        }
        updateState {
            copy(outlineExpandedNodes = newState)
        }
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
    val comments: Map<String, String?> = emptyMap(),
    val snapshotKeys: List<String>? = null,
    val sortedKeys: List<String> = emptyList(),
    val annotationSearchTerm: String = "",
    val annotationFilter: HtmlEpubAnnotationsFilter? = null,
    val annotationPopoverKey: String? = null,
    val annotationPopoverRect: RectF? = null,
    val sidebarEditingEnabled: Boolean = false,
    var outlineSearch: String = "",
    val isTopBarVisible: Boolean = true,
    val showPdfSearch: Boolean = false,
    val showSideBar: Boolean = false,
    val showCreationToolbar: Boolean = false,
    val commentFocusKey: String? = null,
    val commentFocusText: String = "",
    val filter: HtmlEpubAnnotationsFilter? = null,
    val sidebarSliderSelectedOption: HtmlEpubReaderSliderOptions = HtmlEpubReaderSliderOptions.Annotations,
    val isOutlineEmpty: Boolean = false,
    val outlineSearchTerm: String = "",
    val outlineExpandedNodes: Set<String> = emptySet(),
    val outlineSnapshot: List<HtmlEpubOutline> = emptyList(),
) : ViewState {
    fun isAnnotationSelected(annotationKey: String): Boolean {
        return this.selectedAnnotationKey == annotationKey
    }

    fun isOutlineSectionCollapsed(id: String): Boolean {
        val isCollapsed = !outlineExpandedNodes.contains(id)
        return isCollapsed
    }
}

sealed class HtmlEpubReaderViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubReaderViewEffect()
    object DisableForceScreenOn : HtmlEpubReaderViewEffect()
    object EnableForceScreenOn : HtmlEpubReaderViewEffect()
    object ScreenRefresh: HtmlEpubReaderViewEffect()
    data class ScrollSideBar(val scrollToIndex: Int): HtmlEpubReaderViewEffect()
    object ShowPdfFilters : HtmlEpubReaderViewEffect()
}