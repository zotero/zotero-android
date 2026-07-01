package org.zotero.android.screens.reader

import android.app.Activity
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.webkit.WebView
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmObjectChangeListener
import io.realm.RealmResults
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
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
import org.zotero.android.database.requests.CreatePDFAnnotationsDbRequestV2
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
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
import org.zotero.android.pdf.data.PDFDocumentAnnotation
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationArgs
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationColorResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationCommentResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationDeleteResult
import org.zotero.android.screens.reader.annotation.data.ReaderAnnotationScreenClosed
import org.zotero.android.screens.reader.annotationmore.data.ReaderAnnotationMoreArgs
import org.zotero.android.screens.reader.annotationmore.data.ReaderAnnotationMoreDeleteResult
import org.zotero.android.screens.reader.annotationmore.data.ReaderAnnotationMoreSaveResult
import org.zotero.android.screens.reader.colorpicker.data.ReaderColorPickerArgs
import org.zotero.android.screens.reader.colorpicker.data.ReaderColorPickerResult
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.screens.reader.data.ReaderAnnotationsFilter
import org.zotero.android.screens.reader.data.ReaderArgs
import org.zotero.android.screens.reader.data.ReaderDocumentData
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.data.ReaderOutline
import org.zotero.android.screens.reader.data.ReaderPage
import org.zotero.android.screens.reader.data.ReaderWebData
import org.zotero.android.screens.reader.data.ReaderWebError
import org.zotero.android.screens.reader.filter.data.ReaderFilterArgs
import org.zotero.android.screens.reader.filter.data.ReaderFilterResult
import org.zotero.android.screens.reader.search.data.ReaderSearchResultSelected
import org.zotero.android.screens.reader.search.data.ReaderSearchResultsData
import org.zotero.android.screens.reader.search.data.ReaderSearchResultsEventStream
import org.zotero.android.screens.reader.search.data.ReaderSearchTermEventStream
import org.zotero.android.screens.reader.settings.data.PageAppearanceMode
import org.zotero.android.screens.reader.settings.data.ReaderSettings
import org.zotero.android.screens.reader.settings.data.ReaderSettingsArgs
import org.zotero.android.screens.reader.settings.data.ReaderSettingsChangeResult
import org.zotero.android.screens.reader.sidebar.annotations.ReaderAnnotationBitmapManager
import org.zotero.android.screens.reader.sidebar.annotations.cache.ReaderAnnotationBitmapCacheSnapshotEventStream
import org.zotero.android.screens.reader.sidebar.data.ReaderRequestThumbnailRenderEventStream
import org.zotero.android.screens.reader.sidebar.data.ReaderScrollReaderIfNeededEvent
import org.zotero.android.screens.reader.sidebar.data.ReaderSliderOptions
import org.zotero.android.screens.reader.sidebar.data.ReaderWrapperOutline
import org.zotero.android.screens.reader.web.ReaderWebCallChainEventStream
import org.zotero.android.screens.reader.web.ReaderWebCallChainExecutor
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.AnnotationConverterV2
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.getSafeString
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask
import kotlin.math.roundToInt

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val context: Context,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val sessionDataEventStream: SessionDataEventStream,
    private val fileStore: FileStore,
    private val dbWrapperMain: DbWrapperMain,
    private val editItemFieldsDbRequestFactory: EditItemFieldsDbRequest.Factory,
    private val createHtmlEpubAnnotationsDbRequestFactory: CreateHtmlEpubAnnotationsDbRequest.Factory,
    private val createPDFAnnotationsDbRequestFactory: CreatePDFAnnotationsDbRequestV2.Factory,

    private val readerSearchResultsEventStream: ReaderSearchResultsEventStream,
    private val readerSearchTermEventStream: ReaderSearchTermEventStream,
    private val webCallChainEventStream: ReaderWebCallChainEventStream,
    private val readerRequestThumbnailRenderEventStream: ReaderRequestThumbnailRenderEventStream,
    private val annotationBitmapManager: ReaderAnnotationBitmapManager,
    private val annotationBitmapCacheSnapshotEventStream: ReaderAnnotationBitmapCacheSnapshotEventStream,
    private val readerWebCallChainExecutor: ReaderWebCallChainExecutor,

    stateHandle: SavedStateHandle,
) : BaseViewModel2<ReaderViewState, ReaderViewEffect>(ReaderViewState())  {

    private lateinit var originalFile: File
    private lateinit var readerDirectory: File
    private lateinit var documentFile: File
    private lateinit var readerFile: File
    private var userId: Long = 0L
    private var username: String = ""
    private var selectedTextParams: JsonObject? = null
    private var annotations = mutableMapOf<String, NewReaderAnnotation?>()
    private var texts = mutableMapOf<String, Pair<String, Map<TextStyle, String>>?>()
    private val onCommentChangeFlow = MutableStateFlow<Pair<String, String>?>(null)
    private var comments = mutableMapOf<String, String>()
    private val onOutlineSearchStateFlow = MutableStateFlow("")
    private val onAnnotationSearchStateFlow = MutableStateFlow("")
    private lateinit var textFont: TextStyle

    private var isTablet: Boolean = false

    private var disableForceScreenOnTimer: Timer? = null

    private var item: RItem? = null
    private var annotationItems: RealmResults<RItem>? = null

    var outlines: MutableList<ReaderWrapperOutline> = mutableListOf()

    private var annotationEditReaderKey: String? = null

    private var savedSearchTerm = ""

    private var selectedTextParamsText: String = ""

    var activeLineWidth: Float = 0.0f
    var activeEraserSize: Float = 0.0f
    var activeFontSize: Float = 0.0f

    var library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    )
    private var key: String = ""
    private var parentKey: String? = null

    var snapshotKeys: List<String>? = null
    var sidebarEditingEnabled: Boolean = false


    val screenArgs: ReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_READER_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private val screenFileArgs: File by lazy {
        val filePathString = stateHandle.get<String>(ARG_READER_SCREEN_ENCODED_FILE_PATH_ARG).require()
        val file = File(filePathString)
        file
    }

    private var readerSearchTermCancellable: Job? = null
    private var pdfReaderThemeCancellable: Job? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderSearchResultSelected) {
        viewModelScope.launch {
            readerWebCallChainExecutor.selectSearchResult(result.index)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderAnnotationScreenClosed) {
        viewModelScope.launch {
//            _select(null, false)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderAnnotationDeleteResult) {
        val key = result.key
        removeAnnotation(key)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderAnnotationColorResult) {
        val key = result.annotationKey
        val color = result.color
        setColor(color = color, key = key)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(readerAnnotationCommentResult: ReaderAnnotationCommentResult) {
        setComment(readerAnnotationCommentResult.annotationKey, readerAnnotationCommentResult.comment)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderColorPickerResult) {
        viewModelScope.launch {
            setToolOptions(
                hex = result.colorHex,
                tool = result.annotationTool,
                size = result.size
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderFilterResult) {
        viewModelScope.launch {
            set(result.annotationsFilter)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderScrollReaderIfNeededEvent) {
        viewModelScope.launch {
            scrollReaderIfNeeded(result.location, false) {}
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.ReaderScreen) {
            val key = this.annotationEditReaderKey ?: return
            setTags(tags = tagPickerResult.tags, key = key)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderAnnotationMoreSaveResult) {
        set(
            type = result.type,
            color = result.color,
            fontSize = result.fontSize,
            lineWidth = result.lineWidth,
            pageLabel = result.pageLabel,
            text = result.text,
            key = result.key,
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderAnnotationMoreDeleteResult) {
        val key = result.key
        removeAnnotation(key)
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .drop(1)
            .onEach { data ->
                val isDark = data!!.isDark
                updateState {
                    copy(isDark = isDark)
                }
                clearAnnotationsBitmapCache()
                triggerEffect(ReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    private fun setupAnnotationsBitmapCacheUpdateStream() {
        annotationBitmapCacheSnapshotEventStream.flow()
            .onEach { cacheSnapshot ->
                Timber.d("ReaderThumbnailProcessing: thumbnailCache updated")
                updateState {
                    copy(annotationsBitmapCache = cacheSnapshot)
                }
            }
            .launchIn(viewModelScope)
    }


    private fun clearAnnotationsBitmapCache() {
        annotationBitmapManager.cancelProcessing()
    }

    private fun startObservingSearchTerm() {
        this.readerSearchTermCancellable = readerSearchTermEventStream.flow()
            .drop(1)
            .onEach { data ->
                val string = data?.term ?: ""
                savedSearchTerm = string
                if (string.isEmpty()) {
                    readerWebCallChainExecutor.clearSearch()
                    readerSearchResultsEventStream.emitAsync(
                        ReaderSearchResultsData(
                            null
                        )
                    )
                    return@onEach
                }

                readerWebCallChainExecutor.search(string)
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingRequestThumbnailRender() {
        readerRequestThumbnailRenderEventStream.flow()
            .onEach { pageIndices ->
                Timber.d("ReaderThumbnailProcessing: requesting thumbnails on scroll: ${pageIndices}")
                for (i in pageIndices) {
                    readerWebCallChainExecutor.renderThumbnails(i)
                }
            }

            .launchIn(viewModelScope)
    }

    fun initOnce(
        isTablet: Boolean,
        textFont: TextStyle,
    ) = initOnce {
        this.textFont = textFont
        initFileUris()
        copyReaderFiles()
        this.isTablet = isTablet

        EventBus.getDefault().register(this)

        initState()
        setupWebCallChainEventStream()
        startObservingTheme()
        startObservingSearchTerm()
        startObservingRequestThumbnailRender()

        setupCommentChangeFlow()
        setupAnnotationSearchStateFlow()
        setupOutlineSearchStateFlow()

        setupAnnotationsBitmapCacheUpdateStream()
        initAnnotationManager()

        this.activeLineWidth = defaults.getActiveLineWidth()
        this.activeEraserSize = defaults.getActiveEraserSize()
        this.activeFontSize = defaults.getActiveFontSize()

        updateState {
            copy(toolColors = mapOf(
                ReaderAnnotationTool.highlight to defaults.getHighlightColorHex(),
                ReaderAnnotationTool.note to defaults.getNoteColorHex(),
                ReaderAnnotationTool.underline to defaults.getUnderlineColorHex(),

                ReaderAnnotationTool.ink to defaults.getInkColorHex(),
                ReaderAnnotationTool.text to defaults.getTextColorHex(),
                ReaderAnnotationTool.image to defaults.getSquareColorHex(),
            ))
        }
        this.userId = sessionDataEventStream.currentValue()!!.userId
        this.username = defaults.getUsername()

        updatePdfPageAppearanceMode(defaults.getReaderSettings())
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }

    }

    fun initEveryTime(webView: WebView) {
        restartDisableForceScreenOnTimer()
        readerWebCallChainExecutor.start(webView = webView, file = this.readerFile)
    }

    private fun initAnnotationManager() {
        annotationBitmapManager.init(viewModelScope)
        val annotationsBitmapCache = annotationBitmapManager.generateEmptySnapshot().toPersistentMap()
        updateState {
            copy(
                annotationsBitmapCache = annotationsBitmapCache
            )
        }
    }

    private fun copyReaderFiles() {
        val readerUrl = fileStore.readerDirectory()
        readerUrl.copyRecursively(target = readerDirectory, overwrite = true)
        originalFile.copyRecursively(target = documentFile, overwrite = true)
    }

    private fun initState() {
        val params = this.screenArgs
        this.key = params.key
        this.parentKey = params.parentKey
        this.library = params.library
        updateState {
            copy(
                fileType = decideFileType()
            )
        }
    }


    private fun initFileUris() {
        this.originalFile = screenFileArgs
        this.readerDirectory = fileStore.runningReaderDirectory()
        this.documentFile = fileStore.runningReaderUserFileSubDirectory(originalFile.extension)
        this.readerFile = File(readerDirectory, "view.html")

    }

    fun restartDisableForceScreenOnTimer() {
        viewModelScope.launch {
            triggerEffect(ReaderViewEffect.EnableForceScreenOn)
        }
        disableForceScreenOnTimer?.cancel()
        disableForceScreenOnTimer = Timer()
        disableForceScreenOnTimer?.schedule(timerTask {
            viewModelScope.launch {
                triggerEffect(ReaderViewEffect.DisableForceScreenOn)
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
        updateState { copy(isOutlineEmpty = this@ReaderViewModel.outlines.isEmpty()) }
    }

    private fun createSnapshot(search: String) {
        val snapshot = mutableListOf<ReaderWrapperOutline>()
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
        outlines: List<ReaderWrapperOutline>,
        parent: ReaderWrapperOutline?,
        snapshot: MutableList<ReaderWrapperOutline>,
        search: String
    ) {
        val rows = mutableListOf<ReaderWrapperOutline>()
        for (element in outlines) {
            if (search.isEmpty()) {
                val outline = ReaderWrapperOutline(
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

    private fun set(filter: ReaderAnnotationsFilter?) {
        if (filter == viewState.annotationFilter) {
            return
        }
        filterAnnotations(term = viewState.annotationSearchTerm, filter = filter)
    }

    private fun child(children: List<ReaderWrapperOutline>, string: String): Boolean {
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

    private fun outline(outline: ReaderWrapperOutline, string: String): Boolean {
        return (outline.title).contains(string, ignoreCase = true)
    }

    private fun parseOutline(data: JsonObject) {
        val params = data["params"]?.asJsonObject ?: return
        val outline = params["outline"]?.asJsonArray ?: return
        val outlines = mutableListOf<ReaderOutline>()
        for (item in outline) {
            val outline = parseOutline2(item.asJsonObject) ?: continue
            outlines.add(outline)
        }
        this.outlines = outlines.map { ReaderWrapperOutline(it, isActive = true) }.toMutableList()
    }

    fun parseOutline2(data: JsonObject): ReaderOutline? {
        val title = data["title"]?.asString ?: return null
        val location = data["location"]?.asJsonObject ?: return null
        val rawChildren = data["items"]?.asJsonArray ?: return null
        val children = rawChildren.mapNotNull { parseOutline2(it.asJsonObject) }
        return ReaderOutline(
            title = title.trim().trim { it == '\n' },
            location = location.asMap(),
            children = children,
            isActive = true,
        )
    }

    private fun parsePdfJson(pdfAnnotations: JsonArray, author: String, isAuthor: Boolean): List<PDFDocumentAnnotation>  {
        return pdfAnnotations.mapNotNull { pdfAnnotation ->
            val annotation = AnnotationConverterV2.annotation(
                data = pdfAnnotation.asJsonObject,
                author = author,
                isAuthor = isAuthor,
                lineWidthFromUser = 2.0f//TODO fix
            )
            annotation
        }
    }

    private fun parseJsonDate(dateString: String): Date? {
        try {
            return iso8601WithFractionalSeconds.parse(dateString)
        } catch (e: Exception) {
            return iso8601DateFormatV3.parse(dateString)
        }
    }

    private fun parseHtmlEpubJson(
        annotations: JsonArray,
        author: String,
        isAuthor: Boolean
    ): List<NewReaderAnnotation> {
        return annotations.mapNotNull { dataAsJson ->
            val data = dataAsJson.asJsonObject
            val id = data["id"]?.asString ?: return@mapNotNull null
            val dateAdded = (data["dateCreated"]?.asString)?.let {
                parseJsonDate(it)
            }
                ?: return@mapNotNull null

            val dateModified = (data["dateModified"]?.asString)?.let { parseJsonDate(it) }
                ?: return@mapNotNull null
            val color = data["color"]?.asString ?: return@mapNotNull null
            val comment = data["comment"]?.asString ?: return@mapNotNull null //TODO solve
            val pageLabel = data["pageLabel"]?.asString ?: return@mapNotNull null
            val position = data["position"]?.asJsonObject ?: return@mapNotNull null
            val sortIndex = data["sortIndex"]?.asString ?: return@mapNotNull null
            val text = data["text"]?.asString ?: return@mapNotNull null
            val type = (data["type"]?.asString)?.let { AnnotationType.valueOf(it) }
                ?: return@mapNotNull null
            val rawTags = data["tags"]?.asJsonArray ?: return@mapNotNull null
            val tags = rawTags.mapNotNull { dataAsJson ->
                val data = dataAsJson.asJsonObject
                val name = data["name"]?.asString ?: return@mapNotNull null
                val color = data["color"]?.asString ?: return@mapNotNull null
                Tag(name = name, color = color)
            }
            NewReaderAnnotation(
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


    private suspend fun saveAnnotationFromSelection(type: AnnotationType) {
        //TODO support will be added later
        if (viewState.fileType == ReaderFileType.PDF) {
            return
        }

      val textParams =
            this.selectedTextParams?.get("annotation")?.asJsonObject ?: return
        val params = params( textParams, type) ?: return
        val annotations = parseHtmlEpubJson(JsonArray().apply { add(params) }, author =  this.username, isAuthor = true)
        this.selectedTextParams = null
        for (annotation in annotations) {
            this.annotations[annotation.key] = annotation
        }

        readerWebCallChainExecutor.updateView(
            modifications = JsonArray(),
            insertions = JsonArray().apply { add(params) },
            deletions = JsonArray()
        )
        createHtmlEpubDatabaseAnnotations(annotations = annotations)
    }


    private fun params(textParams: JsonObject, type: AnnotationType): JsonObject? {
        val color: String
        when(type) {
            AnnotationType.highlight -> {
                color = viewState.toolColors[ReaderAnnotationTool.highlight] ?: defaults.getHighlightColorHex()
            }
            AnnotationType.underline -> {
                color = viewState.toolColors[ReaderAnnotationTool.underline] ?: defaults.getUnderlineColorHex()
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

    private fun createHtmlEpubDatabaseAnnotations(annotations: List<NewReaderAnnotation>) {
        val request = createHtmlEpubAnnotationsDbRequestFactory.create(
            attachmentKey = this.key,
            libraryId = this.library.identifier,
            annotations = annotations,
            userId = this.userId,
        )
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: could not store annotations")
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

    private fun saveAnnotations(params: JsonObject) {
        val rawAnnotations = params["annotations"]?.asJsonArray
        if (rawAnnotations == null || rawAnnotations.isEmpty()) {
            Timber.e("ReaderViewModel: annotations missing or empty - ${params["annotations"] ?: emptyList<String>()}")
            return
        }
        val annotations = if (viewState.fileType == ReaderFileType.PDF) {
            parsePdfJson(pdfAnnotations = rawAnnotations, author = this.username, isAuthor = true)
        } else {
            parseHtmlEpubJson(annotations = rawAnnotations, author = this.username, isAuthor = true)
        }

        if (annotations.isEmpty()) {
            Timber.e("ReaderViewModel: could not parse annotations")
            return
        }
        if (annotations.size == 1) {
            ignoreDbCallbackOnReaderInsertionItemKey = annotations[0].key
        }

        // Disable annotation tool & select annotation
        val annotation = annotations.firstOrNull {it.type == AnnotationType.note }
        if (annotation != null && viewState.activeTool != null) {
            toggle(viewState.activeTool!!)
        }
        if (viewState.fileType == ReaderFileType.PDF) {
            createPdfDatabaseAnnotations(annotations = annotations as List<PDFDocumentAnnotation>)
        } else {
            createHtmlEpubDatabaseAnnotations(annotations = annotations as List<NewReaderAnnotation>)
        }


        rawAnnotations.forEach {
            val data = it.asJsonObject
            val key = data["id"]?.asString
            val imageBase64 = data["image"]?.asString
            if (key != null && imageBase64 != null) {
                annotationBitmapManager.store(
                    key = key,
                    encodedImageBase64String = imageBase64
                )
            }

        }
    }

    private fun createPdfDatabaseAnnotations(annotations: List<PDFDocumentAnnotation>) {
        val request = createPDFAnnotationsDbRequestFactory.create(
            attachmentKey = this.key,
            libraryId = this.library.identifier,
            annotations = annotations,
            userId = this.userId,
        )
        dbWrapperMain.realmDbStorage.perform(request)

    }

    private suspend fun selectAnnotationFromDocument(key: String) {
        _select(key = key, didSelectInDocument = true)
    }


    private suspend fun _select(key: String?, didSelectInDocument: Boolean) {
        if (key == viewState.selectedAnnotationKey) {
            return
        }
        val existing = viewState.selectedAnnotationKey
        if (existing != null) {
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
            selectAndFocusAnnotationInDocument()
            openAnnotationDialog()
            return
        }

        updateState {
            copy(selectedAnnotationKey = key)
        }

        if (!didSelectInDocument) {
//            selectInDocument(key)
            val annotation = annotation(key)
            if (annotation != null) {
                updateState {
                    copy(
                        focusDocumentLocationAnnotationKey = key
                    )
                }
            }
        } else {
            updateState {
                copy(focusSidebarKey = key)
            }
        }
        selectAndFocusAnnotationInDocument()
        openAnnotationDialog()
    }

    private fun selectInDocument(key: String) {
        viewModelScope.launch {
            readerWebCallChainExecutor.selectInDocument(key)
        }
    }

    private fun setTags(tags: List<Tag>, key: String) {
        val request =
            EditTagsForItemDbRequest(key = key, libraryId = this.library.identifier, tags = tags)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "HReaderViewModel: can't set tags $key")
                updateState {
                    copy(error = Error.cantUpdateAnnotation)
                }
                return@launch
            }
        }
    }

    private fun setComment(key: String, comment: String) {
        val htmlComment = comment //TODO convert to HTML
        this.comments[key] = comment

        val values = mapOf(KeyBaseKeyPair(key = FieldKeys.Item.Annotation.comment, baseKey = null) to htmlComment)
        val request = editItemFieldsDbRequestFactory.create(
            key = key,
            libraryId = this.library.identifier,
            fieldValues = values,
        )

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: can't set comment $key")
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

    private fun deselectSelectedAnnotation() {
        updateState {
            copy(selectedAnnotationKey = null)
        }
    }

    fun toggle(tool: ReaderAnnotationTool) {
        viewModelScope.launch {
            val color = viewState.toolColors[tool]
            toggle(annotationTool = tool, colorHex = color)
        }
    }

    private suspend fun toggle(annotationTool: ReaderAnnotationTool, colorHex: String?) {
        if (viewState.activeTool == annotationTool) {
            updateState {
                copy(activeTool = null)
            }
            readerWebCallChainExecutor.clearTool()
            return
        }
        updateAnnotationToolDrawColorAndSize(annotationTool = annotationTool, colorHex = colorHex)

    }


    private fun filterAnnotations(term: String, filter: ReaderAnnotationsFilter?) {
        if (term.isEmpty() && filter == null) {
            val snapshot = this.snapshotKeys ?: return
            this.snapshotKeys = null
            updateState {
                copy(
                    annotationSearchTerm = "",
                    annotationFilter = null,
                    sortedKeys = snapshot,
                )
            }
            return
        }

        val snapshot = this.snapshotKeys ?: viewState.sortedKeys
        val filteredKeys = filteredKeys(snapshot, term = term, filter = filter)

        if (this.snapshotKeys == null) {
            this.snapshotKeys = viewState.sortedKeys
        }
        updateState {
            copy(
                sortedKeys = filteredKeys,
                annotationSearchTerm = term,
                annotationFilter = filter
            )
        }

    }

    private fun filteredKeys(snapshot: List<String>, term: String?, filter: ReaderAnnotationsFilter?): List<String> {
        if (term == null && filter == null) {
            return snapshot
        }
        return snapshot.filter{ key ->
                val annotation = this.annotations[key] ?:return@filter false
            filter(annotation = annotation, term = term) && filter(annotation = annotation,  filter)
        }
    }

    private fun filter(annotation: NewReaderAnnotation, term: String?): Boolean {
        if (term == null) {
            return true
        }
        return annotation.key.lowercase() == term.lowercase() ||
                annotation.author.contains(term, ignoreCase = true) ||
                annotation.comment.contains(term, ignoreCase = true) ||
                (annotation.text ?: "").contains(term, ignoreCase = true) ||
                annotation.tags.any { it.name.contains(term, ignoreCase = true) }
    }

    private fun filter(annotation: NewReaderAnnotation, filter: ReaderAnnotationsFilter?): Boolean {
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

    private fun loadRawPage(): String {
        val defaultPageValue = defaultPageValue(this.documentFile.extension.lowercase())
        val pageIndexRequest = ReadDocumentDataDbRequest(
            attachmentKey = this.key,
            libraryId = this.library.identifier,
            defaultPageValue = defaultPageValue,
        )
        val pageIndex = dbWrapperMain.realmDbStorage.perform(request = pageIndexRequest)
        return pageIndex
    }

    fun loadItemAnnotations(): Pair<RItem, RealmResults<RItem>>? {
        try {
            val itemRequest =
                ReadItemDbRequest(libraryId = this.library.identifier, key = this.key)
            val item = dbWrapperMain.realmDbStorage.perform(request = itemRequest)

            val annotationsRequest = ReadAnnotationsDbRequest(
                attachmentKey = this.key,
                libraryId = this.library.identifier
            )
            val items = dbWrapperMain.realmDbStorage.perform(request = annotationsRequest)
            return item to items
        } catch (error: Exception) {
            Timber.e(error, "ReaderViewModel: can't load annotations")
            return null
        }
    }


    private fun defaultPageValue(ext: String): String {
        when(ext) {
            "epub" -> {
                return "_start"
            }
            "html", "htm" -> {
                return "0"
            }
            "pdf" -> {
                return "0"
            }
            else -> {
                return ""
            }
        }
    }

    fun loadTypeAndPage(rawPage: String): Pair<String, ReaderPage?> {
        when (this.documentFile.extension.lowercase()) {
            "epub" -> {
                return "epub" to ReaderPage.epub(cfi = rawPage)
            }
            "pdf" -> {
                return "pdf" to ReaderPage.pdf(pageIndex = rawPage.toInt())
            }
            "html", "htm" -> {
                val scrollYPercent = rawPage.toDoubleOrNull()
                if (scrollYPercent != null) {
                    return "snapshot" to ReaderPage.html(scrollYPercent = scrollYPercent)
                } else {
                    Timber.e("ReaderViewModel: incompatible lastIndexPage stored for ${this.key} - $rawPage")
                    return "snapshot" to null
                }
            }
            else -> {
                throw Error.incompatibleDocument
            }
        }
    }
    fun generateReaderInitJsonFromInitialAnnotations(items: RealmResults<RItem>): Triple<List<String>, Map<String, NewReaderAnnotation?>, JsonArray> {
        val sortedKeys = mutableListOf<String>()
        val annotations = mutableMapOf<String, NewReaderAnnotation>()
        val jsons = JsonArray()
        for (item in items) {
            val (annotation, json) = generateAnnotationJsonForReader(item) ?: continue
            jsons.add(json)
            sortedKeys.add(annotation.key)
            annotations[item.key] = annotation
        }
        return Triple(sortedKeys, annotations, jsons)
    }

    private fun decideFileType(): ReaderFileType {
        return when (val extension = this.documentFile.extension.lowercase()) {
            "epub" -> {
                ReaderFileType.EPUB
            }
            "html", "htm" -> {
                ReaderFileType.HTML
            }
            "pdf" -> {
                ReaderFileType.PDF
            }
            else -> {
                throw RuntimeException("Unknown extension $extension")
            }
        }
    }

    private fun generateAnnotationJsonForReader(item: RItem): Pair<NewReaderAnnotation, JsonObject>? {
        val isCurrentFilePdf = viewState.fileType == ReaderFileType.PDF
        return if (isCurrentFilePdf) {
            item.pdfAnnotation
        } else {
            item.newReaderAnnotation
        }
    }

    //When user adds new annotation via reader it gets added to DB, which triggers this callback
    //We need to skip it's execution otherwise it will trigger unnecessary reader update, whichc already has the correct state.
    private var ignoreDbCallbackOnReaderInsertionItemKey : String? = null

    fun update(
        changeSet: OrderedCollectionChangeSet,
        objects: RealmResults<RItem>,
        isInitial: Boolean,
    ) {
        val frozenObjects = objects.freeze()
        val deletions = changeSet.deletions
        var insertions = changeSet.insertions
        val modifications = changeSet.changes

        if (deletions.isEmpty() && insertions.isEmpty() && modifications.isEmpty()) {
            insertions = IntArray(frozenObjects.size) { it }
        }
        Timber.i("ReaderViewModel: annotations changed in database")
        var keys = (this.snapshotKeys ?: viewState.sortedKeys).toMutableList()
        var annotations = this.annotations
        var texts = this.texts
        var comments = this.comments
        var selectionDeleted = false
        var selectKey: String? = null
        var popoverWasInserted = false

        val updatedKeys = mutableListOf<String>()
        val updatedPdfAnnotations = JsonArray()
        val deletedPdfAnnotations = JsonArray()
        val insertedPdfAnnotations = JsonArray()

        for (index in modifications) {
            if (index >= keys.size) {
                Timber.w(
                    "ReaderViewModel: tried modifying index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications"
                )
                continue
            }

            val key = keys[index]
            val item = objects.where().key(key).findFirst() ?: continue
            val (annotation, json) = generateAnnotationJsonForReader(item) ?: continue

            Timber.w("ReaderViewModel: update annotation $key")
            annotations[key] = annotation
            updatedPdfAnnotations.add(json)

            if (canUpdate(key = key, item = item)) {
                Timber.i("ReaderViewModel: update sidebar key $key")
                updatedKeys.add(key)

                if (item.changeType == UpdatableChangeType.sync.name) {
                    Timber.i("ReaderViewModel: update comment")
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
                        AnnotationType.note, AnnotationType.highlight, AnnotationType.image, AnnotationType.underline, AnnotationType.ink -> {
                            comment = annotation.comment //TODO comment attribute conversion
                            comments[key] = comment
                        }

                        AnnotationType.text -> {
                            //no-op, text annotation has no comment
                        }
                    }
                }
            }
        }

        var shouldCancelUpdate = false

        for (index in deletions.reversed()) {
            if (index >= keys.size) {
                Timber.w(
                    "ReaderViewModel: tried removing index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications"
                )
                shouldCancelUpdate = true
                break
            }

            val key = keys.removeAt(index)
            annotations[key] = null
            deletedPdfAnnotations.add(key)
            Timber.i("ReaderViewModel: delete key $key")

            if (viewState.selectedAnnotationKey == key) {
                Timber.i("ReaderViewModel: deleted selected annotation")
                selectionDeleted = true
            }
        }

        if (shouldCancelUpdate) {
            return
        }

        for (index in insertions) {
            if (index > keys.size) {
                Timber.w("ReaderViewModel: tried inserting index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                shouldCancelUpdate = true
                break
            }

            val item = objects[index]!!

            val generatedAnnotation = generateAnnotationJsonForReader(item)
            if (generatedAnnotation == null) {
                Timber.w("ReaderViewModel: tried adding invalid annotation")
                shouldCancelUpdate = true
                break
            }
            val (annotation, json) = generatedAnnotation

            keys.add(index, item.key)
            annotations[item.key] = annotation
            if (viewState.annotationPopoverKey == item.key) {
                popoverWasInserted = true
            }
            Timber.i("ReaderViewModel: insert key ${item.key}")
            when (item.changeType) {
                UpdatableChangeType.user.name -> {
                    //TODO check if sidebar is visible
                    val sidebarVisible = false
                    val isNote =
                        annotation.type == AnnotationType.note
                    if (!this.sidebarEditingEnabled && (sidebarVisible || isNote)) {
                        selectKey = item.key
                        Timber.i("select new annotation")
                    }

                }

                UpdatableChangeType.sync.name, UpdatableChangeType.syncResponse.name -> {
                    insertedPdfAnnotations.add(json)
                    Timber.i("ReaderViewModel: insert annotation")
                }
            }
        }
        if (shouldCancelUpdate) {
            return
        }

        this.annotations = annotations
        this.comments = comments

        updateState {
            if (this@ReaderViewModel.snapshotKeys == null) {
                copy(sortedKeys = keys)
            } else {
                this@ReaderViewModel.snapshotKeys = keys
                copy(
                    sortedKeys = filteredKeys(
                        snapshot = keys,
                        term = viewState.annotationSearchTerm,
                        filter = viewState.annotationFilter
                    )
                )
            }
        }
        viewModelScope.launch {
            if (isInitial) {
                val rawPage = loadRawPage()
                val (type, page) = loadTypeAndPage(rawPage = rawPage)
                val (sortedKeys, annotations, json) = generateReaderInitJsonFromInitialAnnotations(
                    items = objects
                )
                val documentData = ReaderDocumentData(
                    type = type,
                    file = this@ReaderViewModel.documentFile,
                    annotationsJson = json,
                    page = page,
                    selectedAnnotationKey = viewState.selectedAnnotationKey
                )
                readerWebCallChainExecutor.loadDocument(
                    data = documentData,
                    isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark
                )
                restoreWebViewState()
            } else {
                var shouldIgnoreUpdate = false
                if (insertedPdfAnnotations.size() == 1) {
                    val insertedKey = (insertedPdfAnnotations[0].asJsonObject)["id"].asString
                    if (insertedKey == ignoreDbCallbackOnReaderInsertionItemKey) {
                        ignoreDbCallbackOnReaderInsertionItemKey = null
                        shouldIgnoreUpdate = true
                    }
                }
                //TODO remove later if it doesnt lead to new issues
//                if (!shouldIgnoreUpdate) {
                    readerWebCallChainExecutor.updateView(
                        modifications = updatedPdfAnnotations,
                        insertions = insertedPdfAnnotations,
                        deletions = deletedPdfAnnotations
                    )
//                }

            }

            this@ReaderViewModel.texts = texts
            if (popoverWasInserted) {
                //TODO show popover
            }

            val key = selectKey
//            if (key != null) {
//                _select(key = key, didSelectInDocument = true)
//            } else
            if (selectionDeleted) {
                _select(key = null, didSelectInDocument = true)
            }

//        if (selectionDeleted) {
//            _select(key = null, didSelectInDocument = true)
//            updateState {
//                copy(
//                    annotationPopoverKey = null,
//                    annotationPopoverRect = null
//                )
//            }
//            //TODO show popover
//        }
            if ((this@ReaderViewModel.snapshotKeys ?: viewState.sortedKeys).isEmpty()) {
                this@ReaderViewModel.sidebarEditingEnabled = false
            }
            triggerEffect(ReaderViewEffect.ScreenRefresh)
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
                    update(changeSet, items, isInitial = true)
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    update(changeSet, items, isInitial = false)
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "ReaderViewModel: could not load results")
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

    fun setupWebCallChainEventStream() {
        webCallChainEventStream.flow()
            .onEach { result ->
                process(result)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun process(result: Result<ReaderWebData>) {
        if (result is Result.Failure) {
            val customException = (result.exception as? ReaderWebError)?: return
            val errorMessage = when (customException) {
                ReaderWebError.failedToInitializeWebView -> {
                    Timber.e("ReaderViewModel: Worker's JS failed to initialize")
                    context.getSafeString(Strings.retrieve_metadata_error_failed_to_initialize)
                }
            }
            //TODO process error
            return
        }
        val successValue = (result as Result.Success).value
        when (successValue) {
            ReaderWebData.loadDocument -> {
                load()
            }

            is ReaderWebData.parseOutline -> {
                parseOutline(successValue.params)
                loadOutlines()
            }
            ReaderWebData.deselectSelectedAnnotation -> {
                deselectSelectedAnnotation()
            }
            is ReaderWebData.processDocumentSearchResults -> {
                readerSearchResultsEventStream.emitAsync(
                    ReaderSearchResultsData(
                        successValue.params
                    )
                )
            }
            is ReaderWebData.saveAnnotations -> {
                saveAnnotations(successValue.params)
            }
            is ReaderWebData.selectAnnotationFromDocument -> {
                selectAnnotationFromDocument(successValue.key)
            }
            is ReaderWebData.setSelectedTextParams -> {
                setSelectedTextParams(successValue.params)
            }
            is ReaderWebData.setViewState -> {
                setViewState(successValue.params)
            }
            is ReaderWebData.setViewStats -> {
                setViewStats(successValue.params)
            }
            is ReaderWebData.showUrl -> {
                showUrl(successValue.url)
            }
            ReaderWebData.toggleInterfaceVisibility -> {
                decideTopBarAndBottomBarVisibility()
            }

            ReaderWebData.onViewContentInitialized -> {
                val tool = viewState.activeTool
                val color = tool?.let { viewState.toolColors[it] }
                if (tool != null && color != null) {
                    updateAnnotationToolDrawColorAndSize(annotationTool = tool, colorHex = color)
                }
                if (!savedSearchTerm.isEmpty()) {
                    readerWebCallChainExecutor.search(savedSearchTerm)
                }

                updateState {
                    copy(isReaderLoading = false)
                }
            }

            else -> {
                //no-op
            }
        }
    }

    fun setViewState(params: JsonObject) {
        val state = params["state"]?.asJsonObject
        if (state == null) {
            Timber.e("ReaderViewModel: invalid params - $params")
            return
        }

        val page: String

        val pageIndex = state["pageIndex"]
        val scrollPercent = state["scrollYPercent"]
        if (pageIndex != null && pageIndex.isJsonPrimitive) {
            page = pageIndex.asString
        } else if (scrollPercent != null && scrollPercent.isJsonPrimitive) {
            page = "${scrollPercent.asDouble.rounded(1)}"
        } else if (state["cfi"]?.asString != null) {
            page = state["cfi"].asString
        } else {
            return
        }

        if (viewState.fileType == ReaderFileType.PDF) {
            triggerEffect(
                ReaderViewEffect.OnPageChanged(page.toInt())
            )
        }

        val request = StorePageForItemDbRequest(key = this.key, libraryId = this.library.identifier, page = page)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: can't store page")
                return@launch
            }
        }
    }

    private fun setViewStats(params: JsonObject) {
        val stats = params["stats"]?.takeIf { it.isJsonObject }?.asJsonObject
        fun JsonObject.intOrNull(key: String): Int? =
            this[key]?.takeIf { it.isJsonPrimitive }?.asInt
        fun JsonObject.stringOrNull(key: String): String? =
            this[key]?.takeIf { it.isJsonPrimitive }?.asString
        fun JsonObject.boolOrNull(key: String): Boolean? =
            this[key]?.takeIf { it.isJsonPrimitive }?.asBoolean

        val pageIndex = stats?.intOrNull("pageIndex")
        val pagesCount = stats?.intOrNull("pagesCount")
        val pageLabel = stats?.stringOrNull("pageLabel")
        val usePhysicalPageNumbers = stats?.boolOrNull("usePhysicalPageNumbers") ?: false

        // Hide the indicator unless we have what we need to build it: a page
        // (label, or 1-based index) and a percentage (index over total pages).
        val pageProgress = if (pageIndex != null && pagesCount != null && pagesCount > 0) {
            val page = if (!pageLabel.isNullOrBlank()) pageLabel else (pageIndex + 1).toString()
            val prefix = context.getString(
                if (usePhysicalPageNumbers) Strings.page else Strings.location
            )
            val percent = (pageIndex.toDouble() / pagesCount * 100).roundToInt()
            "$prefix $page ($percent%)"
        } else {
            null
        }

        if (viewState.pageProgress != pageProgress) {
            updateState { copy(pageProgress = pageProgress) }
        }
    }

    private fun setSelectedTextParams(params: JsonObject) {
        this.selectedTextParams = params
        val rects = params["rect"].asJsonArray
        this.selectedTextParamsText = (params["annotation"].asJsonObject)["text"].asString
        updateState {
            copy(selectedTextParamsRects = rects)
        }
    }

    fun load() {
        try {

            this.annotations = mutableMapOf()
            this.comments = mutableMapOf()

            this.snapshotKeys = null
            updateState {
                copy(
                    sortedKeys = emptyList()
                )
            }

            val (item, annotationItems) = loadItemAnnotations() ?: return

            if (checkWhetherMd5Changed(item)) {
                return
            }

            this.item?.removeAllChangeListeners()
            this.item = item
            startObservingItem()

            this.annotationItems?.removeAllChangeListeners()
            this.annotationItems = annotationItems
            startObservingAnnotationResults()
        } catch (e: Exception) {
            Timber.e(e, "ReaderViewModel: could not load document")
        }
    }

    suspend fun restoreWebViewState() {
        updateAppearanceAccordingToSettings(false)
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
                ReaderViewEffect.ScrollSideBar(index)
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

    fun annotation(key: String): NewReaderAnnotation? {
        return this.annotations[key]
    }

    fun onCommentFocusFieldChange(annotationKey: String) {
        viewModelScope.launch {
            val annotation =
                annotation(annotationKey)
                    ?: return@launch
            selectAnnotationFromDocument(key = annotationKey)

            updateState {
                copy(
                    commentFocusKey = annotationKey,
                    commentFocusText = annotation.comment
                )
            }
        }
    }

    fun onCommentTextChange(annotationKey: String, comment: String) {
        updateState {
            copy(commentFocusText = comment)
        }
        onCommentChangeFlow.tryEmit(annotationKey to comment)
    }

    private fun searchAnnotations(term: String) {
        val trimmedTerm = term.trim().trim { it == '\n' }
        filterAnnotations(term = trimmedTerm, filter = viewState.annotationFilter)
    }

    fun onAnnotationSearch(text: String) {
        updateState {
            copy(annotationSearchTerm = text)
        }
        onAnnotationSearchStateFlow.tryEmit(text)
    }

    private fun setupAnnotationSearchStateFlow() {
        onAnnotationSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                searchAnnotations(text)
            }
            .launchIn(viewModelScope)
    }

    fun onTagsClicked(annotation: NewReaderAnnotation) {
        viewModelScope.launch {
            selectAnnotationFromDocument(key = annotation.key)

            val selected = annotation.tags.map { it.name }.toSet()

            this@ReaderViewModel.annotationEditReaderKey = annotation.key

            ScreenArguments.tagPickerArgs = TagPickerArgs(
                libraryId = this@ReaderViewModel.library.identifier,
                selectedTags = selected,
                tags = emptyList(),
                callPoint = TagPickerResult.CallPoint.ReaderScreen,
            )

            triggerEffect(ReaderViewEffect.NavigateToTagPickerScreen)
        }

    }

    fun selectAnnotation(key: String) {
        viewModelScope.launch {
            if (!this@ReaderViewModel.sidebarEditingEnabled && key != viewState.selectedAnnotationKey) {
                setCommentActive(true)
                _select(key = key, didSelectInDocument = false)
            }
        }
    }

    fun setSidebarSliderSelectedOption(option: ReaderSliderOptions) {
        updateState {
            copy(sidebarSliderSelectedOption = option)
        }
    }

    fun showFilterPopup() {
        val colors = mutableSetOf<String>()
        val tags = mutableSetOf<Tag>()

        val processAnnotation: (NewReaderAnnotation) -> Unit = { annotation ->
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

        val args = ReaderFilterArgs(
            filter = viewState.annotationFilter,
            availableColors = sortedColors,
            availableTags = sortedTags
        )

        ScreenArguments.readerFilterArgs = args
        if (isTablet) {
            triggerEffect(ReaderViewEffect.ShowPdfFilters)
        } else {
            updateState {
                copy(readerFilterArgs = args)
            }
        }

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

    fun onOutlineItemTapped(outline: ReaderWrapperOutline) {
        viewModelScope.launch {
            if (!outline.isActive) {
                return@launch
            }
            focus(location = outline.location)
            if (!isTablet) {
                toggleSideBar()
            }
        }
    }

    private suspend fun selectAndFocusAnnotationInDocument() {
        val selectedKey = viewState.selectedAnnotationKey
        val annotation = selectedKey?.let { annotation(it) }
        if (annotation != null) {
            val location = viewState.focusDocumentLocationAnnotationKey
            if (location != null) {
                focus(selectedKey)
            } else if (annotation.type != AnnotationType.ink
                || viewState.activeTool != ReaderAnnotationTool.ink
            ) {
                select(selectedKey)
            }
        } else {
            select(null)
        }
    }

    private suspend fun select(
        annotationKey: String?,
    ) {
        if (annotationKey != null) {
            selectInDocument(annotationKey)
        } else {
            //TODO may need to check whether some annotation is actually selected in reader
            //Otherwise might lead to callback loop
            readerWebCallChainExecutor.deselectText()
        }
    }


    fun onOutlineItemChevronTapped(outline: ReaderWrapperOutline) {
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

    fun onMoreOptionsForItemClicked() {
        annotationEditReaderKey = viewState.selectedAnnotationKey

        val selectedAnnotation = annotationEditReaderKey?.let { this.annotations[it] }
        val args = ReaderAnnotationMoreArgs(
            selectedAnnotation = selectedAnnotation,
            library = this.library,
        )
        ScreenArguments.readerAnnotationMoreArgs = args
        if (isTablet) {
            triggerEffect(ReaderViewEffect.ShowReaderAnnotationMore)
        } else {
            updateState {
                copy(readerAnnotationMoreArgs = args)
            }
        }
    }

    fun hidePdfAnnotationMoreView() {
        updateState {
            copy(
                readerAnnotationMoreArgs = null
            )
        }
    }

    private fun set(
        type: AnnotationType,
        color: String,
        fontSize: Float,
        lineWidth: Float,
        pageLabel: String,
        text: String,
        key: String
    ) {
        val values = mapOf(
            KeyBaseKeyPair(
                key = FieldKeys.Item.Annotation.type,
                baseKey = null
            ) to type.name,
            KeyBaseKeyPair(
                key = FieldKeys.Item.Annotation.pageLabel,
                baseKey = null
            ) to pageLabel,
            KeyBaseKeyPair(key = FieldKeys.Item.Annotation.text, baseKey = null) to text,
            KeyBaseKeyPair(key = FieldKeys.Item.Annotation.color, baseKey = null) to color,
            KeyBaseKeyPair(
                key = FieldKeys.Item.Annotation.Position.lineWidth,
                baseKey = FieldKeys.Item.Annotation.position
            ) to "${lineWidth.rounded(3)}",
            KeyBaseKeyPair(
                key = FieldKeys.Item.Annotation.Position.fontSize,
                baseKey = FieldKeys.Item.Annotation.position
            ) to "${fontSize.rounded(3)}",

        )
        val request = editItemFieldsDbRequestFactory.create(
            key = key,
            libraryId = this.library.identifier,
            fieldValues = values,
        )

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: can't set color $key")
                updateState {
                    copy(error = Error.cantUpdateAnnotation)
                }
                return@launch
            }
        }
    }

    private fun removeAnnotation(key: String) {
        //TODO hide popover
        remove(listOf(key))
    }

    private fun remove(keys: List<String>) {
        if(keys.isEmpty()) { return }

        val request = MarkObjectsAsDeletedDbRequest(clazz = RItem::class, keys = keys, libraryId = this.library.identifier)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: can't remove annotations $keys")
                return@launch
            }
        }
    }

    private fun setupCommentChangeFlow() {
        onCommentChangeFlow
            .debounce(500)
            .map { data ->
                if (data != null) {
                    setComment(key = data.first, comment = data.second)
                }
            }
            .launchIn(viewModelScope)
    }

    fun showToolOptions() {
        val tool = viewState.activeTool ?: return
        val colorHex = viewState.toolColors[tool]

        val args = ReaderColorPickerArgs(
            tool = tool,
            colorHex = colorHex,
        )
        ScreenArguments.readerColorPickerArgs = args
        if (isTablet) {
            triggerEffect(ReaderViewEffect.ShowReaderColorPicker)
        } else {
            updateState {
                copy(readerColorPickerArgs = args)
            }
        }
    }

    private suspend fun setToolOptions(hex: String?, size: Float?, tool: ReaderAnnotationTool) {
        if (hex != null) {
            when (tool) {
                ReaderAnnotationTool.highlight -> {
                    defaults.setHighlightColorHex(hex)
                }
                ReaderAnnotationTool.note -> {
                    defaults.setNoteColorHex(hex)
                }
                ReaderAnnotationTool.image -> {
                    defaults.setSquareColorHex(hex)
                }
                ReaderAnnotationTool.ink -> {
                    defaults.setInkColorHex(hex)
                }
                ReaderAnnotationTool.text -> {
                    defaults.setTextColorHex(hex)
                }
                ReaderAnnotationTool.underline -> {
                    defaults.setUnderlineColorHex(hex)
                }

                ReaderAnnotationTool.eraser -> {
                    //no-op
                }
            }
        }
        if (size != null) {
            when (tool) {
                ReaderAnnotationTool.eraser -> {
                    defaults.setActiveEraserSize(size)
                }
                ReaderAnnotationTool.ink -> {
                    defaults.setActiveLineWidth(size)
                }
                ReaderAnnotationTool.text -> {
                    defaults.setActiveFontSize(size)
                }
                else -> {
                    //no-op
                }
            }
        }
        if (hex != null) {
            updateState {
                copy(toolColors = viewState.toolColors + (tool to hex))
            }
        }
        if (size != null) {
            when (tool) {
                ReaderAnnotationTool.ink -> {
                    this.activeLineWidth = size
                }
                ReaderAnnotationTool.eraser -> {
                    this.activeEraserSize = size
                }
                ReaderAnnotationTool.text -> {
                    this.activeFontSize = size
                }
                else -> {
                    //no-op
                }
            }
        }
        updateAnnotationToolDrawColorAndSize(annotationTool = tool, colorHex = hex)
    }

    private suspend fun updateAnnotationToolDrawColorAndSize(
        annotationTool: ReaderAnnotationTool,
        colorHex: String? = null
    ) {
        readerWebCallChainExecutor.clearTool()

        when (annotationTool) {
            ReaderAnnotationTool.ink -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "ink",
                    colorHex = colorHex!!,
                    size = this.activeLineWidth
                )
            }

            ReaderAnnotationTool.text -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "text",
                    colorHex = colorHex!!,
                    size = this.activeFontSize
                )
            }

            ReaderAnnotationTool.highlight -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "highlight",
                    colorHex = colorHex!!,
                )
            }

            ReaderAnnotationTool.note -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "note",
                    colorHex = colorHex!!,
                )
            }

            ReaderAnnotationTool.image -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "image",
                    colorHex = colorHex!!,
                )
            }

            ReaderAnnotationTool.underline -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "underline",
                    colorHex = colorHex!!,
                )
            }

            ReaderAnnotationTool.eraser -> {
                readerWebCallChainExecutor.setTool(
                    toolName = "eraser",
                    size = this.activeEraserSize
                )
            }
        }
        _select(key = null, didSelectInDocument = true)
        updateState {
            copy(activeTool = annotationTool)
        }
        triggerEffect(ReaderViewEffect.ScreenRefresh)
    }



    private fun openAnnotationDialog() {
        val showAnnotationPopup = !viewState.showSideBar && viewState.selectedAnnotationKey != null
        if (showAnnotationPopup) {
            annotationEditReaderKey = viewState.selectedAnnotationKey
            val selectedAnnotation = annotationEditReaderKey?.let { this.annotations[it] }
            val readerAnnotationArgs = ReaderAnnotationArgs(
                selectedAnnotation = selectedAnnotation,
                library = this.library
            )
            ScreenArguments.readerAnnotationArgs = readerAnnotationArgs
            if (!isTablet) {
                updateState {
                    copy(readerAnnotationArgs = readerAnnotationArgs)
                }
            }
        }

        val index = viewState.sortedKeys.indexOf(viewState.selectedAnnotationKey)
        triggerEffect(
            ReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList(
                index,
                showAnnotationPopup
            )
        )
    }


    fun hideAnnotationView() {
//        clearSelectedAnnotations()
        updateState {
            copy(
                readerAnnotationArgs = null
            )
        }
    }

    private fun setColor(color: String, key: String) {
        val values =
            mapOf(KeyBaseKeyPair(key = FieldKeys.Item.Annotation.color, baseKey = null) to color)
        val request = editItemFieldsDbRequestFactory.create(
            key = key,
            libraryId = this.library.identifier,
            fieldValues = values,
        )

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "ReaderViewModel: can't set color $key")
                updateState {
                    copy(error = Error.cantUpdateAnnotation)
                }
                return@launch
            }
        }
    }

    private var previousSidebarVisibilityState = false

    private fun decideTopBarAndBottomBarVisibility() = viewModelScope.launch {
        val isTopBarCurrentlyVisible = viewState.isTopBarVisible
        val topBarNewVisibilityState = !isTopBarCurrentlyVisible
        setTopBarVisibility(topBarNewVisibilityState)
        delay(300)//First hide topbar and then with a delay sidebar, creates much nicer visual effect
        if (!topBarNewVisibilityState) {
            this@ReaderViewModel.previousSidebarVisibilityState = viewState.showSideBar
            updateState { copy(showSideBar = false) }
        } else {
            updateState { copy(showSideBar = this@ReaderViewModel.previousSidebarVisibilityState) }
        }
    }

    fun navigateToReaderSettings() {
        val args = ReaderSettingsArgs(
            readerSettings = defaults.getReaderSettings(),
            fileType = viewState.fileType
        )
        val params = navigationParamsMarshaller.encodeObjectToBase64(args)
        if (isTablet) {
            triggerEffect(ReaderViewEffect.ShowReaderSettings(params))
        } else {
            updateState {
                copy(readerSettingsArgs = args)
            }
        }
    }

    fun hideSettingsView() {
        updateState {
            copy(
                readerSettingsArgs = null
            )
        }
    }

    fun hideColorPickerView() {
        updateState {
            copy(
                readerColorPickerArgs = null
            )
        }
    }

    fun hideFilterView() {
        updateState {
            copy(
                readerFilterArgs = null
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: ReaderSettingsChangeResult) {
        viewModelScope.launch {
            update(result.readerSettings)
        }
    }

    private fun update(readerSettings: ReaderSettings) {
        defaults.setReaderSettings(readerSettings)
        updatePdfPageAppearanceMode(readerSettings)
        updateAppearanceAccordingToSettings(true)
    }

    private fun updateAppearanceAccordingToSettings(isSettingsUpdate: Boolean) {
        viewModelScope.launch {
            val readerSettings = defaults.getReaderSettings()
            if (isSettingsUpdate) {
                readerWebCallChainExecutor.updateInterface(pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
                if (viewState.fileType == ReaderFileType.PDF || viewState.fileType == ReaderFileType.EPUB) {
                    readerWebCallChainExecutor.setSpreadMode(readerSettings.spreadsMode)
                }
                if (viewState.fileType == ReaderFileType.EPUB) {
                    readerWebCallChainExecutor.setFlowMode(readerSettings.pageLayoutFlowMode)
                }
            }

        }
    }

    private fun updatePdfPageAppearanceMode(readerSettings: ReaderSettings) {
        val oldPageAppearanceMode = when (readerSettings.appearanceMode) {
            PageAppearanceMode.LIGHT -> {
                org.zotero.android.pdf.data.PageAppearanceMode.LIGHT
            }

            PageAppearanceMode.DARK -> {
                org.zotero.android.pdf.data.PageAppearanceMode.DARK
            }

            PageAppearanceMode.AUTOMATIC -> {
                org.zotero.android.pdf.data.PageAppearanceMode.AUTOMATIC
            }
        }

        pdfReaderThemeDecider.setPdfPageAppearanceMode(oldPageAppearanceMode)
    }

    private fun showUrl(url: String) {
        triggerEffect(ReaderViewEffect.OpenWebpage(url))
    }

    fun dismissActionMenu() {
        updateState {
            copy(selectedTextParamsRects = null)
        }
        viewModelScope.launch {
            readerWebCallChainExecutor.deselectText()
        }
    }

    fun onCopy() {
        dismissActionMenu()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", selectedTextParamsText)
        clipboard.setPrimaryClip(clip)
    }

    fun onShare(localActivity: Activity?) {
        dismissActionMenu()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, selectedTextParamsText)
        }
        localActivity?.startActivity(Intent.createChooser(intent, "Share text"))
    }
    fun onTranslate(localActivity: Activity?) {
        dismissActionMenu()

        val intent = Intent(Intent.ACTION_PROCESS_TEXT)
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, selectedTextParamsText)
        intent.putExtra(
            Intent.EXTRA_PROCESS_TEXT_READONLY,
            false
        )
        intent.setType("text/plain")
        localActivity?.startActivity(intent)
    }
    fun onWebSearch(localActivity: Activity?) {
        dismissActionMenu()

        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra(SearchManager.QUERY, selectedTextParamsText)
        localActivity?.startActivity(intent)
    }

    fun onHighlight() = viewModelScope.launch {
        saveAnnotationFromSelection(AnnotationType.highlight)
        dismissActionMenu()
    }

    fun onUnderline() = viewModelScope.launch {
        saveAnnotationFromSelection(AnnotationType.underline)
        dismissActionMenu()
    }

    private suspend fun scrollReaderIfNeeded(location: Map<String, Any>, animated: Boolean, completion: () -> Unit) {
//        val locationsPage = location["pageNumber"] ?: location["pageIndex"]
//        if (isCurrentFilePdf() && viewState.currentPdfPageIndex.toString() == locationsPage as String) {
//            completion()
//            return
//        }
        if (!animated) {
            readerWebCallChainExecutor.show(location = location)
            completion()
            return
        }
        readerWebCallChainExecutor.show(location = location)
        completion()
    }

    private suspend fun focus(location: Map<String, Any>) {
        scrollReaderIfNeeded(location, animated = true, completion = {})
    }

    private suspend fun focus(
        annotationKey: String,
    ) {
//        scrollIfNeeded(pageIndex, true) {
             select(annotationKey = annotationKey)
//        }
    }

}

data class ReaderViewState(
    val selectedTextParamsRects: JsonArray? = null,
    val isDark: Boolean = false,
    val error: ReaderViewModel.Error? = null,
    val activeTool: ReaderAnnotationTool? = null,
    val selectedAnnotationKey: String? = null,
    val selectedAnnotationCommentActive: Boolean = false,
    val focusSidebarKey: String? = null,
    val sortedKeys: List<String> = emptyList(),
    val annotationSearchTerm: String = "",
    val annotationFilter: ReaderAnnotationsFilter? = null,
    val annotationPopoverKey: String? = null,
    val annotationPopoverRect: RectF? = null,
    var outlineSearch: String = "",
    val isTopBarVisible: Boolean = true,
    val showPdfSearch: Boolean = false,
    val showSideBar: Boolean = false,
    val showCreationToolbar: Boolean = false,
    val commentFocusKey: String? = null,
    val commentFocusText: String = "",
    val sidebarSliderSelectedOption: ReaderSliderOptions = ReaderSliderOptions.Annotations,
    val isOutlineEmpty: Boolean = false,
    val outlineSearchTerm: String = "",
    val outlineExpandedNodes: Set<String> = emptySet(),
    val outlineSnapshot: List<ReaderWrapperOutline> = emptyList(),
    val readerAnnotationMoreArgs: ReaderAnnotationMoreArgs? = null,
    val readerAnnotationArgs: ReaderAnnotationArgs? = null,
    val readerSettingsArgs: ReaderSettingsArgs? = null,
    val readerColorPickerArgs: ReaderColorPickerArgs? = null,
    val readerFilterArgs: ReaderFilterArgs? = null,
    val toolColors: Map<ReaderAnnotationTool, String> = emptyMap(),
    val focusDocumentLocationAnnotationKey: String? = null,
    val annotationsBitmapCache: PersistentMap<String, Bitmap> = persistentMapOf(),
    val pageProgress: String? = null,
    val fileType: ReaderFileType = ReaderFileType.EPUB,
    val isReaderLoading: Boolean = true,
    ) : ViewState {
    fun isAnnotationSelected(annotationKey: String): Boolean {
        return this.selectedAnnotationKey == annotationKey
    }

    fun isOutlineSectionCollapsed(id: String): Boolean {
        val isCollapsed = !outlineExpandedNodes.contains(id)
        return isCollapsed
    }

    fun isPdfOrHtml(): Boolean {
        return fileType == ReaderFileType.PDF || fileType == ReaderFileType.HTML
    }


}

sealed class ReaderViewEffect : ViewEffect {
    object NavigateBack : ReaderViewEffect()
    object DisableForceScreenOn : ReaderViewEffect()
    object EnableForceScreenOn : ReaderViewEffect()
    object ScreenRefresh : ReaderViewEffect()
    data class ScrollSideBar(val scrollToIndex: Int) : ReaderViewEffect()
    object ShowPdfFilters : ReaderViewEffect()
    object ShowReaderAnnotationMore : ReaderViewEffect()
    object NavigateToTagPickerScreen : ReaderViewEffect()
    object ShowReaderColorPicker : ReaderViewEffect()
    data class ShowReaderSettings(val params: String) : ReaderViewEffect()
    data class ShowPdfAnnotationAndUpdateAnnotationsList(
        val scrollToIndex: Int,
        val showAnnotationPopup: Boolean
    ) : ReaderViewEffect()

    data class OpenWebpage(val url: String) : ReaderViewEffect()
    data class OnPageChanged(val currentPage: Int) : ReaderViewEffect()
}