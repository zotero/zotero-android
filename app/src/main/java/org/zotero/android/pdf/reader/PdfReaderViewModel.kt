package org.zotero.android.pdf.reader

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pspdfkit.R
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.BlendMode
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.TextMarkupAnnotation
import com.pspdfkit.annotations.UnderlineAnnotation
import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.annotations.configuration.EraserToolConfiguration
import com.pspdfkit.annotations.configuration.FreeTextAnnotationConfiguration
import com.pspdfkit.annotations.configuration.InkAnnotationConfiguration
import com.pspdfkit.annotations.configuration.MarkupAnnotationConfiguration
import com.pspdfkit.annotations.configuration.NoteAnnotationConfiguration
import com.pspdfkit.annotations.configuration.ShapeAnnotationConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.OutlineElement
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import com.pspdfkit.ui.search.SearchResultHighlighter
import com.pspdfkit.ui.special_mode.controller.AnnotationCreationController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import org.zotero.android.ZoteroApplication
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.database.objects.zoteroType
import org.zotero.android.database.requests.CreatePDFAnnotationsDbRequest
import org.zotero.android.database.requests.EditAnnotationFontSizeDbRequest
import org.zotero.android.database.requests.EditAnnotationPathsDbRequest
import org.zotero.android.database.requests.EditAnnotationRectsDbRequest
import org.zotero.android.database.requests.EditAnnotationRotationDbRequest
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.StorePageForItemDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.ktx.annotation
import org.zotero.android.ktx.baseColor
import org.zotero.android.ktx.index
import org.zotero.android.ktx.isZoteroAnnotation
import org.zotero.android.ktx.key
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.ARG_PDF_SCREEN
import org.zotero.android.pdf.annotation.data.PdfAnnotationArgs
import org.zotero.android.pdf.annotation.data.PdfAnnotationColorResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationCommentResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationDeleteResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationSizeResult
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreArgs
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreDeleteResult
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreSaveResult
import org.zotero.android.pdf.cache.AnnotationPreviewCacheUpdatedEventStream
import org.zotero.android.pdf.cache.AnnotationPreviewFileCache
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerArgs
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerResult
import org.zotero.android.pdf.colorpicker.queuedUpPdfReaderColorPickerResult
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.AnnotationPreviewManager
import org.zotero.android.pdf.data.AnnotationsFilter
import org.zotero.android.pdf.data.PDFDatabaseAnnotation
import org.zotero.android.pdf.data.PDFDocumentAnnotation
import org.zotero.android.pdf.data.PDFSettings
import org.zotero.android.pdf.data.PageFitting
import org.zotero.android.pdf.data.PageLayoutMode
import org.zotero.android.pdf.data.PageScrollDirection
import org.zotero.android.pdf.data.PdfAnnotationChanges
import org.zotero.android.pdf.data.PdfReaderArgs
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchArgs
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchResultSelected
import org.zotero.android.pdf.reader.sidebar.data.Outline
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderOutlineOptionsWithChildren
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderSliderOptions
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderThumbnailRow
import org.zotero.android.pdf.reader.sidebar.data.ThumbnailPreviewCacheUpdatedEventStream
import org.zotero.android.pdf.reader.sidebar.data.ThumbnailPreviewManager
import org.zotero.android.pdf.reader.sidebar.data.ThumbnailPreviewMemoryCache
import org.zotero.android.pdf.reader.sidebar.data.ThumbnailsPreviewFileCache
import org.zotero.android.pdf.settings.data.PdfSettingsArgs
import org.zotero.android.pdf.settings.data.PdfSettingsChangeResult
import org.zotero.android.pdffilter.data.PdfFilterArgs
import org.zotero.android.pdffilter.data.PdfFilterResult
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.AnnotationBoundingBoxCalculator
import org.zotero.android.sync.AnnotationColorGenerator
import org.zotero.android.sync.AnnotationConverter
import org.zotero.android.sync.AnnotationSplitter
import org.zotero.android.sync.DateParser
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import timber.log.Timber
import java.util.EnumSet
import javax.inject.Inject
import kotlin.random.Random


@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapperMain: DbWrapperMain,
    private val sessionDataEventStream: SessionDataEventStream,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val annotationPreviewManager: AnnotationPreviewManager,
    private val annotationPreviewFileCache: AnnotationPreviewFileCache,
    private val thumbnailPreviewManager: ThumbnailPreviewManager,
    private val thumbnailsPreviewFileCache: ThumbnailsPreviewFileCache,
    override val thumbnailPreviewMemoryCache: ThumbnailPreviewMemoryCache,
    private val context: Context,
    private val annotationPreviewCacheUpdatedEventStream: AnnotationPreviewCacheUpdatedEventStream,
    private val thumbnailPreviewCacheUpdatedEventStream: ThumbnailPreviewCacheUpdatedEventStream,
    override val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache,
    private val schemaController: SchemaController,
    private val dateParser: DateParser,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<PdfReaderViewState, PdfReaderViewEffect>(PdfReaderViewState()), PdfReaderVMInterface {

    private var liveAnnotations: RealmResults<RItem>? = null
    private var databaseAnnotations: RealmResults<RItem>? = null
    private lateinit var annotationBoundingBoxConverter: AnnotationBoundingBoxConverter
    private var containerId = 0
    private lateinit var uri: Uri
    private lateinit var pdfUiFragment: PdfUiFragment
    private lateinit var pdfFragment: PdfFragment
    private var onAnnotationUpdatedListener: AnnotationProvider.OnAnnotationUpdatedListener? = null
    private lateinit var document: PdfDocument
    private lateinit var rawDocument: PdfDocument
    var comments = mutableMapOf<String, String>()
    private val onAnnotationSearchStateFlow = MutableStateFlow("")
    private val onAnnotationChangedDebouncerFlow = MutableStateFlow<Triple<Int, List<String>, FreeTextAnnotation>?>(null)
    private val onOutlineSearchStateFlow = MutableStateFlow("")
    private val onCommentChangeFlow = MutableStateFlow<Pair<String, String>?>(null)
    private lateinit var fragmentManager: FragmentManager
    private var isTablet: Boolean = false

    private val handler = Handler(context.mainLooper)

    //Used to recreate a new fragment preserving viewport state
    private lateinit var pdfDocumentBeforeFragmentDestruction: PdfDocument

    override var annotationMaxSideSize = 0

    override var toolColors: MutableMap<AnnotationTool, String> = mutableMapOf()
    var changedColorForTool: AnnotationTool? = null
    var activeLineWidth: Float = 0.0f
    var activeEraserSize: Float = 0.0f
    var activeFontSize: Float = 0.0f

    private var toolHistory = mutableListOf<AnnotationTool>()

    private lateinit var searchResultHighlighter: SearchResultHighlighter

    val screenArgs: PdfReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_PDF_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserInterfaceVisibilityChanged(event: OnUserInterfaceVisibilityChangedEvent) {
        toggleTopAndBottomBarVisibility(event.isVisible)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPdfPageScrolled(event: OnPdfPageScrolled) {
        triggerEffect(PdfReaderViewEffect.ScrollThumbnailListToIndex(event.pageIndex))
        val row = viewState.thumbnailRows.firstOrNull { it.pageIndex == event.pageIndex }
        updateState {
            copy(selectedThumbnail = row)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationMoreSaveResult) {
        set(
            color = result.color,
            lineWidth = result.lineWidth,
            fontSize = result.fontSize,
            pageLabel = result.pageLabel,
            updateSubsequentLabels = result.updateSubsequentLabels,
            text = result.text,
            key = result.key.key,
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.PdfReaderScreen) {
            val annotation = this@PdfReaderViewModel.selectedAnnotation ?: return
            set(tags = tagPickerResult.tags, key = annotation.key)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(pdfAnnotationCommentResult: PdfAnnotationCommentResult) {
        setComment(pdfAnnotationCommentResult.annotationKey, pdfAnnotationCommentResult.comment)
        if (isTablet) {
            // Fix for a bug where selecting an already selected annotation again didn't trigger Annotation Edit Popup/Screen.
            // Unfortunately PSPDFKIT's onAnnotationSelected method is not triggered when user is selecting the same annotation again. Because technically the same annotation just stays selected.
            // That's why after finishing annotation editing we have to make PSPDFKIT to deselect the currently selected annotation.
            // Drawback to this is that of course visually annotation gets deselected as well.
            this.pdfFragment.clearSelectedAnnotations()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationSizeResult) {
        setLineWidth(key = result.key, width = result.size)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationMoreDeleteResult) {
        val key = viewState.selectedAnnotationKey ?: return
        remove(key)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationDeleteResult) {
        val key = viewState.selectedAnnotationKey ?: return
        remove(key)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationColorResult) {
        setColor(key = result.annotationKey, color = result.color)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: EventBusConstants.OnKeyboardVisibilityChange) {
        viewModelScope.launch {
            if (!result.isOpen) {
                triggerEffect(PdfReaderViewEffect.ClearFocus)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfFilterResult) {
        viewModelScope.launch {
            set(result.annotationsFilter)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfSettingsChangeResult) {
        viewModelScope.launch {
            update(result.pdfSettings)
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfReaderColorPickerResult) {
        if (isTablet) {
            viewModelScope.launch {
                setToolOptions(
                    hex = result.colorHex,
                    size = result.size,
                    tool = result.annotationTool
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfReaderSearchResultSelected) {
        searchResultHighlighter.setSearchResults(listOf(result.searchResult))
        searchResultHighlighter.setSelectedSearchResult(result.searchResult)
        this.pdfUiFragment.pageIndex = result.searchResult.pageIndex
    }

    private fun update(pdfSettings: PDFSettings) {
        defaults.setPDFSettings(pdfSettings)
        pdfReaderThemeDecider.setPdfPageAppearanceMode(pdfSettings.appearanceMode)
        if (isTablet) {
            pdfDocumentBeforeFragmentDestruction = pdfFragment.document!!
            replaceFragment()
        }
    }

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                val isDark = data!!.isDark
                updateState {
                    copy(isDark = isDark)
                }
                thumbnailPreviewMemoryCache.clear()
                triggerEffect(PdfReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    class CustomPdfUiFragment: PdfUiFragment() {
        override fun onUserInterfaceVisibilityChanged(visible: Boolean) {
            super.onUserInterfaceVisibilityChanged(visible)
            EventBus.getDefault().post(OnUserInterfaceVisibilityChangedEvent(visible))
        }

        override fun onPageChanged(document: PdfDocument, pageIndex: Int) {
            super.onPageChanged(document, pageIndex)
            EventBus.getDefault().post(OnPdfPageScrolled(pageIndex))
        }
    }

    data class OnUserInterfaceVisibilityChangedEvent(val isVisible: Boolean)
    data class OnPdfPageScrolled(val pageIndex: Int)

    override fun init(
        uri: Uri,
        annotationMaxSideSize: Int,
        containerId: Int,
        fragmentManager: FragmentManager,
        isTablet: Boolean,
    ) {
        this.uri = uri
        this.isTablet = isTablet
        this.fragmentManager = fragmentManager
        this.containerId = containerId
        this.annotationMaxSideSize = annotationMaxSideSize

        searchResultHighlighter = SearchResultHighlighter(context)

        if (this::pdfUiFragment.isInitialized) {
            replaceFragment()
            return
        }

        EventBus.getDefault().register(this)

        initState()
        startObservingTheme()
        setupAnnotationCacheUpdateStream()
        setupThumbnailCacheUpdateStream()
        setupAnnotationSearchStateFlow()
        setupOutlineSearchStateFlow()
        setupCommentChangeFlow()
        setupAnnotationChangedDebouncerFlow()

        val pdfSettings = defaults.getPDFSettings()
        pdfReaderThemeDecider.setPdfPageAppearanceMode(pdfSettings.appearanceMode)
        val configuration = generatePdfConfiguration(pdfSettings)
        this@PdfReaderViewModel.pdfUiFragment = PdfUiFragmentBuilder
            .fromUri(context, uri)
            .fragmentClass(CustomPdfUiFragment::class.java)
            .configuration(configuration)
            .build()
        this@PdfReaderViewModel.pdfUiFragment.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                this@PdfReaderViewModel.pdfFragment = pdfUiFragment.pdfFragment!!
                this@PdfReaderViewModel.pdfFragment.addDrawableProvider(searchResultHighlighter)
                addDocumentListenerOnInit()
                addOnAnnotationCreationModeChangeListener()
                setOnPreparePopupToolbarListener()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                pdfUiFragment.lifecycle.removeObserver(this)
            }
        })

        fragmentManager.commit {
            add(containerId, this@PdfReaderViewModel.pdfUiFragment)
        }
    }

    private fun addDocumentListenerOnInit() {
        this@PdfReaderViewModel.pdfFragment.addDocumentListener(object :
            DocumentListener {
            override fun onDocumentLoaded(document: PdfDocument) {
                this@PdfReaderViewModel.onDocumentLoaded(document)
            }

            override fun onPageClick(
                document: PdfDocument,
                pageIndex: Int,
                event: MotionEvent?,
                pagePosition: PointF?,
                clickedAnnotation: Annotation?
            ): Boolean {
                return false
            }
        })
    }

    private fun setOnPreparePopupToolbarListener() {
        this.pdfFragment.setOnPreparePopupToolbarListener { toolbar ->
            val sourceItems = toolbar.menuItems
            val menuItems = sourceItems.listIterator()

            while (menuItems.hasNext()) {
                val item = menuItems.next()
                when (item.id) {
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_underline,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_strikeout,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_speak,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_search,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_redact,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_paste_annotation,
                    com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_link,
                    -> {
                        menuItems.remove()
                    }
                }
            }

            toolbar.menuItems = sourceItems
        }
    }

    private fun setColor(key: String, color: String) {
        setC(key = key, color = color)
    }

    private fun setC(color: String, key:String) {
        val annotation = annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return
        update(annotation = annotation, color = (color to viewState.isDark), document = this.document)
    }

    private fun setF(fontSize: Float, key: String) {
        val annotation = annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return
        update(annotation = annotation, fontSize = fontSize, document = this.document)
    }


    fun set(selected: Boolean) {
        updateState {
            copy(isColorPickerButtonVisible = selected)
        }
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }


    private fun onDocumentLoaded(document: PdfDocument) {
        this.document = document
        annotationBoundingBoxConverter = AnnotationBoundingBoxConverter(document)
        loadRawDocument()
        loadDocumentData()
        setupInteractionListeners()
        loadOutlines()
        loadThumbnails()
    }

    private fun loadThumbnails() {
        val rows = mutableListOf<PdfReaderThumbnailRow>()
        for (i in 0..<document.pageCount) {
            rows.add(
                PdfReaderThumbnailRow(
                    pageIndex = i,
                    title = this.document.getPageLabel(i, true)
                )
            )
        }
        updateState {
            copy(thumbnailRows = rows.toImmutableList())
        }

    }

    private fun loadOutlines() {
        createSnapshot("")
        updateState { copy(isOutlineEmpty = document.outline.isEmpty()) }
    }

    private fun setupAnnotationSearchStateFlow() {
        onAnnotationSearchStateFlow
            .debounce(150)
            .map { text ->
                searchAnnotations(text)
            }
            .launchIn(viewModelScope)
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

    private fun setupAnnotationCacheUpdateStream() {
        annotationPreviewCacheUpdatedEventStream.flow()
            .onEach {
//                notifyItemChanged(viewModel.viewState.sortedKeys.indexOfFirst { it.key == key })
                triggerEffect(
                    PdfReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList(
                        scrollToIndex = -1,
                        showAnnotationPopup = false
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    private fun setupThumbnailCacheUpdateStream() {
        thumbnailPreviewCacheUpdatedEventStream.flow()
            .onEach { pageIndex ->
                val existingRow = viewState.thumbnailRows.firstOrNull { it.pageIndex == pageIndex }
                if (existingRow != null) {
                    val updatedRow = existingRow.copyAndUpdateLoadedState()
                    val modifiedList = viewState.thumbnailRows.toMutableList()
                    val indexToReplace = modifiedList.indexOf(existingRow)
                    modifiedList[indexToReplace] = updatedRow
                    updateState {
                        copy(thumbnailRows = modifiedList.toImmutableList())
                    }
                }
                triggerEffect(PdfReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    private fun loadRawDocument() {
        this.rawDocument =
            PdfDocumentLoader.openDocument(context, this.document.documentSource.fileUri!!)
    }

    private fun setupInteractionListeners() {
        pdfFragment.addOnAnnotationSelectedListener(object :
            AnnotationManager.OnAnnotationSelectedListener {
            override fun onPrepareAnnotationSelection(
                p0: AnnotationSelectionController,
                p1: Annotation,
                p2: Boolean
            ): Boolean {
                //no-op
                return true
            }

            override fun onAnnotationSelected(annotation: Annotation, p1: Boolean) {
                val key = annotation.key ?: annotation.uuid
                val type: AnnotationKey.Kind =
                    if (annotation.isZoteroAnnotation) AnnotationKey.Kind.database else AnnotationKey.Kind.document
                selectAnnotationFromDocument(AnnotationKey(key = key, type = type))
            }

        })
        pdfFragment.addOnAnnotationDeselectedListener { annotation, _ ->
            deselectSelectedAnnotation(annotation)
        }
    }

    private fun initState() {
        val params = this.screenArgs
        val username = defaults.getUsername()
        val userId = sessionDataEventStream.currentValue()!!.userId
        val displayName = defaults.getDisplayName()

        this.toolColors = mutableMapOf(
            AnnotationTool.HIGHLIGHT to defaults.getHighlightColorHex(),
            AnnotationTool.SQUARE to defaults.getSquareColorHex(),
            AnnotationTool.NOTE to defaults.getNoteColorHex(),
            AnnotationTool.INK to defaults.getInkColorHex(),
            AnnotationTool.UNDERLINE to defaults.getUnderlineColorHex(),
            AnnotationTool.FREETEXT to defaults.getTextColorHex(),
        )

        this.activeLineWidth = defaults.getActiveLineWidth()
        this.activeEraserSize = defaults.getActiveEraserSize()
        this.activeFontSize = defaults.getActiveFontSize()

        updateState {
            copy(
                key = params.key,
                library = params.library,
                userId = userId,
                username = username,
                displayName = displayName,
                visiblePage = 0,
                initialPage = params.page,
                selectedAnnotationKey = params.preselectedAnnotationKey?.let {
                    AnnotationKey(
                        key = it,
                        type = AnnotationKey.Kind.database
                    )
                }
            )
        }
    }

    private fun loadAnnotationsAndPage(
        key: String,
        library: Library
    ): CustomResult<Pair<RealmResults<RItem>, Int>> {
        try {
            var pageStr = "0"
            var results: RealmResults<RItem>? = null
            dbWrapperMain.realmDbStorage.perform { coordinator ->
                pageStr = coordinator.perform(
                    request = ReadDocumentDataDbRequest(
                        attachmentKey = key,
                        libraryId = library.identifier
                    )
                )
                results = coordinator.perform(
                    request = ReadAnnotationsDbRequest(
                        attachmentKey = key,
                        libraryId = library.identifier
                    )
                )
            }
            val page = pageStr.toIntOrNull()
            if (page == null) {
                return CustomResult.GeneralError.CodeError(Exception("Can't add annotations"))//TODO pageNotInt
            }

            return CustomResult.GeneralSuccess(results!! to page)
        } catch (e: Exception) {
            Timber.e(e)
            return CustomResult.GeneralError.CodeError(e)
        }
    }

    private fun loadAnnotations(
        document: PdfDocument,
        username: String,
        displayName: String
    ): Map<String, PDFDocumentAnnotation> {
        val annotations = mutableMapOf<String, PDFDocumentAnnotation>()
        val pdfAnnotations = document.annotationProvider
            .getAllAnnotationsOfTypeAsync(AnnotationsConfig.supported)
            .toList()
            .blockingGet()

        for (pdfAnnotation in pdfAnnotations) {
            if (pdfAnnotation is SquareAnnotation && !pdfAnnotation.isZoteroAnnotation) {
                continue
            }
            val annotation = AnnotationConverter.annotation(
                document = document,
                annotation = pdfAnnotation,
                color = pdfAnnotation.color.toHexString(),
                username = username,
                displayName = displayName,
                boundingBoxConverter = this.annotationBoundingBoxConverter
            ) ?: continue

            annotations[annotation.key] = annotation
        }

        return annotations

    }

    private fun loadDocumentData() {
        val key = viewState.key
        val library = viewState.library
        val dbResult = loadAnnotationsAndPage(key = key, library = library)

        when (dbResult) {
            is CustomResult.GeneralSuccess -> {
                this.liveAnnotations?.removeAllChangeListeners()
                this.liveAnnotations = dbResult.value!!.first
                val storedPage = dbResult.value!!.second
                observe(liveAnnotations!!)
                this.databaseAnnotations = liveAnnotations!!.freeze()
                val documentAnnotations = loadAnnotations(
                    this.document,
                    username = viewState.username,
                    displayName = viewState.displayName
                )
                val dbToPdfAnnotations = AnnotationConverter.annotations(
                    this.databaseAnnotations!!,
                    isDarkMode = false,
                    currentUserId = viewState.userId,
                    library = library,
                    displayName = viewState.displayName,
                    username = viewState.username,
                    boundingBoxConverter = annotationBoundingBoxConverter
                )
                val sortedKeys = createSortedKeys(
                    databaseAnnotations = databaseAnnotations!!,
                    pdfDocumentAnnotations = documentAnnotations
                )

                update(
                    document = this.document,
                    zoteroAnnotations = dbToPdfAnnotations,
                    key = key,
                    libraryId = library.identifier,
                    isDark = viewState.isDark
                )
                for (annotation in dbToPdfAnnotations) {
                    annotationPreviewManager.store(
                        rawDocument = this.rawDocument,
                        annotation = annotation,
                        parentKey = key,
                        libraryId = library.identifier,
                        isDark = viewState.isDark,
                        annotationMaxSideSize = annotationMaxSideSize
                    )
                }

                val (page, selectedData) = preselectedData(
                    databaseAnnotations = databaseAnnotations!!,
                    storedPage = storedPage,
                    boundingBoxConverter = annotationBoundingBoxConverter
                )

                updateState {
                    copy(
                        pdfDocumentAnnotations = documentAnnotations,
                        sortedKeys = sortedKeys,
                        visiblePage = page,
                        initialPage = null,
                    )
                }

                this.pdfUiFragment.pageIndex = page

                if (selectedData != null) {
                    val (key, location) = selectedData
                    updateState {
                        copy(
                            selectedAnnotationKey = key,
                            focusDocumentLocation = location,
                            focusSidebarKey = key
                        )
                    }
                }
            }

            is CustomResult.GeneralError.CodeError -> {
                Timber.e(dbResult.throwable)
            }

            else -> {}
        }
        observeDocument()
        updateAnnotationsList(forceNotShowAnnotationPopup = true)
    }

    private fun setupAnnotationChangedDebouncerFlow() {
        onAnnotationChangedDebouncerFlow
            .debounce(200)
            .map { pair ->
                if (pair != null) {
                    change(annotation = pair.third, pair.second)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeDocument() {
        onAnnotationUpdatedListener = object :
            AnnotationProvider.OnAnnotationUpdatedListener {
            override fun onAnnotationCreated(annotation: Annotation) {
                processAnnotationObserving(annotation, emptyList(), PdfReaderNotification.PSPDFAnnotationsAdded)
            }

            override fun onAnnotationUpdated(annotation: Annotation) {
                processAnnotationObserving(annotation, emptyList(), PdfReaderNotification.PSPDFAnnotationChanged)
            }

            override fun onAnnotationRemoved(annotation: Annotation) {
                processAnnotationObserving(annotation, emptyList(), PdfReaderNotification.PSPDFAnnotationsRemoved)
            }

            override fun onAnnotationZOrderChanged(
                p0: Int,
                p1: MutableList<Annotation>,
                p2: MutableList<Annotation>
            ) {
                //no-op
            }
        }
        pdfFragment.addOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)
    }

    private fun change(annotation: Annotation, changes: List<String>) {
        if (changes.isEmpty()) {
            return
        }
        val key = annotation.key ?: return

        annotationPreviewManager.store(
            rawDocument = rawDocument,
            annotation = annotation,
            parentKey = viewState.key,
            libraryId = viewState.library.identifier,
            isDark = viewState.isDark,
            annotationMaxSideSize = this.annotationMaxSideSize
        )
        thumbnailPreviewManager.store(
            pageIndex = annotation.pageIndex,
            key = viewState.key,
            document = this.document,
            libraryId = viewState.library.identifier,
            isDark = viewState.isDark,
        )

        val hasChanges: (List<PdfAnnotationChanges>) -> Boolean = hasChangesScope@{ pdfChanges ->
            val rawPdfChanges = PdfAnnotationChanges.stringValues(pdfChanges)
            for (change in changes) {
                if (rawPdfChanges.contains(change)) {
                    return@hasChangesScope true
                }
            }
            false
        }

        Timber.i("PdfReaderViewModel: annotation changed - $key; $changes")

        var requests = mutableListOf<DbRequest>()
        val inkAnnotation = annotation as? InkAnnotation
        val textAnnotation = annotation as? FreeTextAnnotation
        if (inkAnnotation != null) {
            if (hasChanges(listOf(PdfAnnotationChanges.paths, PdfAnnotationChanges.boundingBox))) {
                val paths = AnnotationConverter.paths(inkAnnotation)
                requests.add(
                    EditAnnotationPathsDbRequest(
                        key = key,
                        libraryId = viewState.library.identifier,
                        paths = paths,
                        boundingBoxConverter = this.annotationBoundingBoxConverter
                    )
                )
            }

            if (hasChanges(listOf(PdfAnnotationChanges.lineWidth))) {
                val values = mapOf(
                    KeyBaseKeyPair(
                        key = FieldKeys.Item.Annotation.Position.lineWidth,
                        baseKey = FieldKeys.Item.Annotation.position
                    ) to "${inkAnnotation.lineWidth.rounded(3)}"
                )
                val request = EditItemFieldsDbRequest(
                    key = key,
                    libraryId = viewState.library.identifier,
                    fieldValues = values,
                    dateParser = this.dateParser,
                )
                requests.add(request)
            }
        } else if (textAnnotation != null) {
            var editFontSize = hasChanges(listOf(PdfAnnotationChanges.fontSize))
            if (hasChanges(listOf(PdfAnnotationChanges.boundingBox))) {
                val rects = AnnotationConverter.rects(annotation)
                if (rects != null) {
                    requests.add(
                        EditAnnotationRectsDbRequest(
                            key = key,
                            libraryId = viewState.library.identifier,
                            rects = rects,
                            boundingBoxConverter = this.annotationBoundingBoxConverter
                        )
                    )
                    editFontSize = true
                }
            }

            if (hasChanges(listOf(PdfAnnotationChanges.rotation))) {
                requests.add(
                    EditAnnotationRotationDbRequest(
                        key = key,
                        libraryId = viewState.library.identifier,
                        rotation = 360 - textAnnotation.rotation
                    )
                )
            }
            if (editFontSize) {
                requests.add(
                    EditAnnotationFontSizeDbRequest(
                        key = key,
                        libraryId = viewState.library.identifier,
                        size = textAnnotation.textSize.toInt()
                    )
                )
            }
        }

        else if (hasChanges(listOf(PdfAnnotationChanges.boundingBox, PdfAnnotationChanges.rects))) {
            val rects = AnnotationConverter.rects(annotation)
            if (rects != null) {
                requests.add(
                    EditAnnotationRectsDbRequest(
                        key = key,
                        libraryId = viewState.library.identifier,
                        rects = rects,
                        boundingBoxConverter = this.annotationBoundingBoxConverter
                    )
                )
            }
        }

        if (hasChanges(listOf(PdfAnnotationChanges.color))) {
            val values = mapOf(
                KeyBaseKeyPair(
                    key = FieldKeys.Item.Annotation.color,
                    baseKey = null
                ) to annotation.baseColor
            )
            val request = EditItemFieldsDbRequest(
                key = key,
                libraryId = viewState.library.identifier,
                fieldValues = values,
                dateParser = this.dateParser,
            )
            requests.add(request)
        }

        if (hasChanges(listOf(PdfAnnotationChanges.contents))) {
            val values = mapOf(
                KeyBaseKeyPair(
                    key = FieldKeys.Item.Annotation.comment,
                    baseKey = null
                ) to (annotation.contents ?: "")
            )
            val request = EditItemFieldsDbRequest(
                key = key,
                libraryId = viewState.library.identifier,
                fieldValues = values,
                dateParser = this.dateParser,
            )
            requests.add(request)
        }

        if(requests.isEmpty()) { return }

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                writeRequests = requests
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel:  can't update changed annotations")
                return@launch
            }
        }

        pdfFragment.notifyAnnotationHasChanged(annotation)
        //TODO
    }

    private fun processAnnotationObserving(
        annotation: Annotation,
        changes: List<String>,
        pdfReaderNotification: PdfReaderNotification
    ) {

        when (pdfReaderNotification) {
            PdfReaderNotification.PSPDFAnnotationChanged -> {
                when (annotation) {
                    is FreeTextAnnotation -> {
                        val adjustedAnnotations: List<String> = if (changes.isEmpty()) {
                            PdfAnnotationChanges.stringValues(
                                listOf(
                                    PdfAnnotationChanges.boundingBox,
                                    PdfAnnotationChanges.fontSize,
                                    PdfAnnotationChanges.rotation,
                                    PdfAnnotationChanges.contents,
                                )
                            )
                        } else {
                            changes
                        }
                        val key = annotation.key
                        if (key != null) {
//                        if (changes.contains("rotation") || freeTextAnnotation.rotation != 0) {
                            onAnnotationChangedDebouncerFlow.tryEmit(
                                Triple(
                                    Random.nextInt(),
                                    adjustedAnnotations,
                                    annotation
                                )
                            )
//                        } else {
//                            val k = onAnnotationChangedDebouncerFlow.value
//                            if (k != null) {
//                                change(k.second, k.first)
//                            }
//                            change(annotation = annotation, changes = changes)
//                        }
                        } else {
                            change(annotation = annotation, changes = adjustedAnnotations)
                        }
                    }
                    else -> {
                        val listOfChanges = mutableListOf(
                            PdfAnnotationChanges.boundingBox,
                            PdfAnnotationChanges.paths
                        )
                        if (annotation is UnderlineAnnotation) {
                            listOfChanges.add(PdfAnnotationChanges.color)
                        }
                        change(
                            annotation = annotation,
                            changes = PdfAnnotationChanges.stringValues(
                                listOfChanges
                            )
                        )
                    }
                }
            }
            PdfReaderNotification.PSPDFAnnotationsAdded -> {
                add(listOf(annotation))
            }
            PdfReaderNotification.PSPDFAnnotationsRemoved -> {
                remove(annotations = listOf(annotation))
            }
        }
    }

    private fun preselectedData(
        databaseAnnotations: RealmResults<RItem>,
        storedPage: Int,
        boundingBoxConverter: AnnotationBoundingBoxConverter
    ): Pair<Int, Pair<AnnotationKey, Pair<Int, RectF>>?> {
        val key = viewState.selectedAnnotationKey
        if (key != null) {
            val item = databaseAnnotations.where().key(key.key).findFirst()
            if (item != null) {
                val annotation = PDFDatabaseAnnotation.init(item = item)
                if (annotation != null) {
                    val page = annotation._page ?: storedPage
                    val boundingBox =
                        annotation.boundingBox(boundingBoxConverter = boundingBoxConverter)
                    return page to (key to (page to boundingBox))
                }
            }
        }

        val initialPage = viewState.initialPage
        if (initialPage != null && initialPage >= 0 && initialPage < this.document.pageCount) {
            return initialPage to null
        }

        return storedPage to null
    }

    private fun update(
        document: PdfDocument,
        zoteroAnnotations: List<Annotation>,
        key: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ) {
        val allAnnotations = document.annotationProvider.getAllAnnotationsOfType(
            EnumSet.allOf(
                AnnotationType::class.java
            )
        )
        for (annotation in allAnnotations) {
            annotation.flags =
                EnumSet.copyOf(annotation.flags + AnnotationFlags.LOCKED)
            annotationPreviewManager.store(
                rawDocument = this.rawDocument,
                annotation = annotation,
                parentKey = key,
                libraryId = libraryId,
                isDark = isDark,
                annotationMaxSideSize = annotationMaxSideSize
            )
        }
        zoteroAnnotations.forEach {
            document.annotationProvider.addAnnotationToPage(it)
        }
    }

    private fun createSortedKeys(
        databaseAnnotations: RealmResults<RItem>,
        pdfDocumentAnnotations: Map<String, PDFDocumentAnnotation>
    ): List<AnnotationKey> {
        val sortMap = mapOf<String, AnnotationKey>().toSortedMap()
        for (item in databaseAnnotations) {
            val annotation = PDFDatabaseAnnotation.init(item = item)
            if (annotation == null || !validate(annotation)) {
                continue
            }
            sortMap[item.annotationSortIndex] = AnnotationKey(
                key = item.key,
                type = AnnotationKey.Kind.database
            )
        }
        for (annotation in pdfDocumentAnnotations.values) {
            val key = AnnotationKey(key = annotation.key, type = AnnotationKey.Kind.document)
            val sortIndex = annotation.sortIndex
            sortMap[sortIndex] = key
        }
        val result = sortMap.map { it.value }
        return result
    }

    private fun validate(databaseAnnotation: PDFDatabaseAnnotation): Boolean {
        if (databaseAnnotation._page == null) {
            return false
        }
        when (databaseAnnotation.type) {
            org.zotero.android.database.objects.AnnotationType.ink -> {
                if (databaseAnnotation.item.paths.isEmpty()) {
                    Timber.i("PDFReaderActionHandler: ink annotation ${databaseAnnotation.key} missing paths")
                    return false
                }
            }

            org.zotero.android.database.objects.AnnotationType.note,
            org.zotero.android.database.objects.AnnotationType.highlight,
            org.zotero.android.database.objects.AnnotationType.image,
            org.zotero.android.database.objects.AnnotationType.underline
            -> {
                if (databaseAnnotation.item.rects.isEmpty()) {
                    Timber.i("PDFReaderActionHandler: ${databaseAnnotation.type} annotation ${databaseAnnotation.key} missing rects")
                    return false
                }
            }
            org.zotero.android.database.objects.AnnotationType.text -> {
                if (databaseAnnotation.item.rects.isEmpty()) {
                    Timber.i("PDFReaderActionHandler: ${databaseAnnotation.type} annotation ${databaseAnnotation.key} missing rects")
                    return false
                }
                if (databaseAnnotation.fontSize == null) {
                    Timber.i("PDFReaderActionHandler: ${databaseAnnotation.type} annotation ${databaseAnnotation.key} missing fontSize")
                    return false
                }
                if (databaseAnnotation.rotation == null) {
                    Timber.i("PDFReaderActionHandler: ${databaseAnnotation.type} annotation ${databaseAnnotation.key} missing rotation")
                    return false
                }
            }
        }

        val sortIndex = databaseAnnotation.sortIndex
        val parts = sortIndex.split("|")
        if (parts.size != 3 || parts[0].length != 5 || parts[1].length != 6 || parts[2].length != 5) {
            Timber.i("PDFReaderActionHandler: invalid sort index (${sortIndex}) for ${databaseAnnotation.key}")
            return false
        }

        return true
    }


    private fun observe(results: RealmResults<RItem>) {
        results.addChangeListener { objects, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    update(
                        objects = objects,
                        deletions = deletions,
                        insertions = insertions,
                        modifications = modifications
                    )
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "PdfReaderViewModel: could not load results")
                }

                else -> {
                    //no-op
                }
            }
        }
    }

    private fun update(
        objects: RealmResults<RItem>,
        deletions: IntArray,
        insertions: IntArray,
        modifications: IntArray
    ) {
        Timber.i("PdfReaderViewModel: database annotation changed")

        val keys = (viewState.snapshotKeys
            ?: viewState.sortedKeys).filter { it.type == AnnotationKey.Kind.database }
            .toMutableList()
        var comments = this.comments
        var selectKey: AnnotationKey? = null
        var selectionDeleted = false

        var updatedKeys = mutableListOf<AnnotationKey>()
        var updatedPdfAnnotations = mutableMapOf<Annotation, PDFDatabaseAnnotation>()
        var deletedPdfAnnotations = mutableListOf<Annotation>()
        var insertedPdfAnnotations = mutableListOf<Annotation>()

        for (index in modifications) {
            if (index >= keys.size) {
                Timber.w("Tried modifying index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                continue
            }
            val key = keys[index]
            val item = objects.where().key(key.key).findFirst() ?: continue
            val annotation = PDFDatabaseAnnotation.init(item = item) ?: continue

            if (canUpdate(key = key, item = item, index = index)) {
                Timber.i("update key $key")
                updatedKeys.add(key)

                if (item.changeType == UpdatableChangeType.sync.name) {
                    Timber.i("update comment")
                    comments[key.key] = annotation.comment
                }
            }

            if (item.changeType != UpdatableChangeType.sync.name) {
                continue
            }

            val pdfAnnotation = this.document.annotationProvider.getAnnotations(annotation.page)
                .firstOrNull { it.key == key.key } ?: continue
            Timber.i("update PDF annotation")
            updatedPdfAnnotations[pdfAnnotation] = annotation
        }

        var shouldCancelUpdate = false

        for (index in deletions.reversed()) {
            if (index >= keys.size) {
                Timber.w("tried removing index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                shouldCancelUpdate = true
                break
            }

            val key = keys.removeAt(index)
            Timber.i("delete key $key")

            if (viewState.selectedAnnotationKey == key) {
                Timber.i("deleted selected annotation")
                selectionDeleted = true
            }

            val oldAnnotation = PDFDatabaseAnnotation.init(item = this.databaseAnnotations!![index]!!) ?: continue
            val pdfAnnotation =
                this.document.annotationProvider.getAnnotations(oldAnnotation.page)
                    .firstOrNull { it.key == oldAnnotation.key } ?: continue
            Timber.i("delete PDF annotation")
            deletedPdfAnnotations.add(pdfAnnotation)
        }

        if (shouldCancelUpdate) {
            return
        }

        for (index in insertions) {
            if (index > keys.size) {
                Timber.w("tried inserting index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                shouldCancelUpdate = true
                break
            }
            val item = objects[index]!!
            keys.add(
                element = AnnotationKey(key = item.key, type = AnnotationKey.Kind.database),
                index = index
            )
            Timber.i("PDFReaderActionHandler: insert key ${item.key}")

            val annotation = PDFDatabaseAnnotation.init(item = item)
            if (annotation == null ){
                Timber.w("PdfReaderViewModel: tried inserting unsupported annotation (${item.annotationType})! keys.count=${keys.size}; index=${index}; deletions=${deletions}; insertions=${insertions}; modifications=${modifications}")
                shouldCancelUpdate = true
                break
            }

            when (item.changeType) {
                UpdatableChangeType.user.name -> {
                    //TODO check if sidebar is visible
                    val sidebarVisible = false
                    val isNote =
                        annotation.type == org.zotero.android.database.objects.AnnotationType.note
                    if (!viewState.sidebarEditingEnabled && (sidebarVisible || isNote)) {
                        selectKey =
                            AnnotationKey(key = item.key, type = AnnotationKey.Kind.database)
                        Timber.i("select new annotation")
                    }

                }

                UpdatableChangeType.sync.name, UpdatableChangeType.syncResponse.name -> {
                    val pdfAnnotation = AnnotationConverter.annotation(
                        zoteroAnnotation = annotation,
                        type = AnnotationConverter.Kind.zotero,
                        isDarkMode = viewState.isDark,
                        currentUserId = viewState.userId,
                        library = viewState.library,
                        displayName = viewState.displayName,
                        username = viewState.username,
                        boundingBoxConverter = annotationBoundingBoxConverter
                    )
                    insertedPdfAnnotations.add(pdfAnnotation)
                    Timber.i("insert PDF annotation")
                }
            }
        }

        if (shouldCancelUpdate) {
            return
        }

        val getSortIndex: (AnnotationKey) -> String? = { key ->
            when (key.type) {
                AnnotationKey.Kind.document -> {
                    viewState.pdfDocumentAnnotations[key.key]?.sortIndex
                }

                AnnotationKey.Kind.database -> {
                    objects.where().key(key.key).findFirst()?.annotationSortIndex
                }
            }
        }
        for (annotation in viewState.pdfDocumentAnnotations.values) {
            val key = AnnotationKey(key = annotation.key, type = AnnotationKey.Kind.document)
            val index = keys.index(key, sortedBy = { lKey, rKey ->
                val lSortIndex = getSortIndex(lKey) ?: ""
                val rSortIndex = getSortIndex(rKey) ?: ""
                lSortIndex < rSortIndex
            })
            keys.add(element = key, index = index)
        }
        pdfFragment.removeOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)

        for ((pdfAnnotation, annotation) in updatedPdfAnnotations) {
            update(
                pdfAnnotation = pdfAnnotation,
                annotation = annotation,
                parentKey = viewState.key,
                libraryId = viewState.library.identifier,
                isDarkMode = viewState.isDark
            )
        }
        if (!deletedPdfAnnotations.isEmpty()) {
            for (annotation in deletedPdfAnnotations) {
                if (annotation.flags.contains(AnnotationFlags.READONLY)) {
                    annotation.flags =
                        EnumSet.copyOf(annotation.flags - AnnotationFlags.READONLY)
                }
                annotationPreviewManager.delete(
                    annotation = annotation,
                    parentKey = viewState.key,
                    libraryId = viewState.library.identifier
                )
            }
            deletedPdfAnnotations.forEach {
                this.document.annotationProvider.removeAnnotationFromPage(it)
            }
        }

        if (!insertedPdfAnnotations.isEmpty()) {
            insertedPdfAnnotations.forEach {
                this.document.annotationProvider.addAnnotationToPage(it)
                annotationPreviewManager.store(
                    rawDocument = this.rawDocument,
                    annotation = it,
                    parentKey = viewState.key,
                    libraryId = viewState.library.identifier,
                    isDark = viewState.isDark,
                    annotationMaxSideSize = annotationMaxSideSize
                )
            }
        }
        val pageIndicesForThumbnails =
            (deletedPdfAnnotations.map { it.pageIndex } + insertedPdfAnnotations.map { it.pageIndex }).toSet()
        pageIndicesForThumbnails.forEach { pageIndex ->
            thumbnailPreviewManager.store(
                pageIndex = pageIndex,
                key = viewState.key,
                document = this.document,
                libraryId = viewState.library.identifier,
                isDark = viewState.isDark,
            )
        }

        observeDocument()
        this.comments = comments
        this.databaseAnnotations = objects.freeze()
        updateAnnotationsList(forceNotShowAnnotationPopup = true)
        if (viewState.snapshotKeys != null) {
            updateState {
                copy(
                    snapshotKeys = keys,
                    sortedKeys = keys //TODO filter keys
                )
            }
        } else {
            updateState {
                copy(
                    sortedKeys = keys
                )
            }
        }
        updateState {
            copy(updatedAnnotationKeys = updatedKeys.filter { viewState.sortedKeys.contains(it) })
        }
        val key = selectKey
        if (key != null) {
            _select(key = key, didSelectInDocument = true)
        } else if (selectionDeleted) {
            _select(key = null, didSelectInDocument = true)
        }

        if ((viewState.snapshotKeys ?: viewState.sortedKeys).isEmpty()) {
            updateState {
                copy(sidebarEditingEnabled = false)
            }
        }

    }

    private fun _select(key: AnnotationKey?, didSelectInDocument: Boolean) {
        val existing = viewState.selectedAnnotationKey
        if (existing != null) {
            if (viewState.sortedKeys.contains(existing)) {
                val updatedAnnotationKeys =
                    (viewState.updatedAnnotationKeys ?: emptyList()).toMutableList()
                updatedAnnotationKeys.add(existing)
                updateState {
                    copy(updatedAnnotationKeys = updatedAnnotationKeys)
                }
            }

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
            updateAnnotationsList()
            return
        }

        updateState {
            copy(selectedAnnotationKey = key)
        }

        if (!didSelectInDocument) {
            val annotation = annotation(key)
            if (annotation != null) {
                updateState {
                    copy(
                        focusDocumentLocation = (annotation.page to annotation.boundingBox(
                            boundingBoxConverter = annotationBoundingBoxConverter
                        ))
                    )
                }
            }
        } else {
            updateState {
                copy(focusSidebarKey = key)
            }
        }

        if (viewState.sortedKeys.contains(key)) {
            val updatedAnnotationKeys =
                (viewState.updatedAnnotationKeys ?: emptyList()).toMutableList()
            updatedAnnotationKeys.add(key)
            updateState {
                copy(updatedAnnotationKeys = updatedAnnotationKeys)
            }
        }
        selectAndFocusAnnotationInDocument()
        updateAnnotationsList()
    }

    private fun updateAnnotationsList(forceNotShowAnnotationPopup: Boolean = false) {
        hidePspdfkitToolbars()
        var showAnnotationPopup = !forceNotShowAnnotationPopup && !viewState.showSideBar && selectedAnnotation != null
        if (selectedAnnotation?.type == org.zotero.android.database.objects.AnnotationType.text) {
            showAnnotationPopup = false
        }
        if (showAnnotationPopup) {
            ScreenArguments.pdfAnnotationArgs = PdfAnnotationArgs(
                selectedAnnotation = selectedAnnotation,
                userId = viewState.userId,
                library = viewState.library
            )
        }

        val index = viewState.sortedKeys.indexOf(viewState.selectedAnnotationKey)
        triggerEffect(
            PdfReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList(
                index,
                showAnnotationPopup
            )
        )

    }

    private fun hidePspdfkitToolbars() {
        handler.postDelayed({
            val editingToolbarView = pdfUiFragment
                .view?.rootView?.findViewById<View>(R.id.pspdf__annotation_editing_toolbar)
            editingToolbarView?.visibility = View.GONE
            val creationToolbarView = pdfUiFragment
                .view?.rootView?.findViewById<View>(R.id.pspdf__annotation_creation_toolbar)
            creationToolbarView?.visibility = View.GONE
        }, 200)
    }

    private fun selectAndFocusAnnotationInDocument() {
        val annotation = this.selectedAnnotation
        if (annotation != null) {
            val location = viewState.focusDocumentLocation
            if (location != null) {
                focus(annotation = annotation, location = location, document = this.document)
            } else if (annotation.type != org.zotero.android.database.objects.AnnotationType.ink || pdfFragment.activeAnnotationTool?.toAnnotationType() != AnnotationType.INK) {
                val pageIndex = pdfUiFragment.pageIndex
                select(annotation = annotation, pageIndex = pageIndex, document = this.document)
            }
        } else {
            select(annotation = null, pageIndex = pdfUiFragment.pageIndex, document = this.document)
        }
    }

    fun focus(page: Int) {
        scrollIfNeeded(page, animated = true, completion = {})
    }

    private fun focus(
        annotation: org.zotero.android.pdf.data.PDFAnnotation,
        location: Pair<Int, RectF>,
        document: PdfDocument
    ) {
        val pageIndex = annotation.page
        scrollIfNeeded(pageIndex, true) {
            select(annotation = annotation, pageIndex = pageIndex, document = document)
        }
    }

    private fun scrollIfNeeded(pageIndex: Int, animated: Boolean, completion: () -> Unit) {
        if (pdfUiFragment.pageIndex == pageIndex) {
            completion()
            return
        }

        if (!animated) {
            pdfUiFragment.setPageIndex(pageIndex, false)
            completion()
            return
        }
        pdfUiFragment.setPageIndex(pageIndex, true)
        completion()
    }


    private fun select(
        annotation: org.zotero.android.pdf.data.PDFAnnotation?,
        pageIndex: Int,
        document: PdfDocument
    ) {

        //TODO updateSelection

        if (annotation != null) {
            val pdfAnnotation = document.annotation(pageIndex, annotation.key)
            if (pdfAnnotation != null) {
                if (!pdfFragment.selectedAnnotations.contains(pdfAnnotation)) {
                    pdfFragment.setSelectedAnnotation(pdfAnnotation)
                    val zoomScale = pdfFragment.getZoomScale(pageIndex)
                    if (zoomScale > 1.0) {
                        pdfFragment.scrollTo(pdfAnnotation.boundingBox, pageIndex, 100, false)
                    }
                }
            } else {
                if (!pdfFragment.selectedAnnotations.isEmpty()) {
                    pdfFragment.clearSelectedAnnotations()
                }
            }
        } else {
            if (!pdfFragment.selectedAnnotations.isEmpty()) {
                pdfFragment.clearSelectedAnnotations()
            }
        }
    }

    override fun annotation(key: AnnotationKey): org.zotero.android.pdf.data.PDFAnnotation? {
        when (key.type) {
            AnnotationKey.Kind.database -> {
                return this.databaseAnnotations!!.where().key(key.key).findFirst()
                    ?.let { PDFDatabaseAnnotation.init(item = it) }
            }

            AnnotationKey.Kind.document -> {
                return viewState.pdfDocumentAnnotations[key.key]
            }
        }
    }

    private fun update(
        annotation: org.zotero.android.pdf.data.PDFAnnotation,
        color: Pair<String, Boolean>? = null,
        lineWidth: Float? = null,
        fontSize: Float? = null,
        contents: String? = null,
        document: PdfDocument
    ) {
        val pdfAnnotation = document.annotationProvider.getAnnotations(annotation.page)
            .firstOrNull { it.key == annotation.key } ?: return

        val changes = mutableListOf<PdfAnnotationChanges>()

        if (lineWidth != null && lineWidth.rounded(3) != annotation.lineWidth) {
            changes.add(PdfAnnotationChanges.lineWidth)
        }

        if (fontSize != null && fontSize != annotation.fontSize) {
            changes.add(PdfAnnotationChanges.fontSize)
        }

        if (color != null && color.first != annotation.color) {
            changes.add(PdfAnnotationChanges.color)
        }
        if (contents != null && contents != annotation.comment) {
            changes.add(PdfAnnotationChanges.contents)
        }

        if (changes.isEmpty()) {
            return
        }
        //Android's PSPDFKit seems to not have "recordCommand" functionality
        if (changes.contains(PdfAnnotationChanges.lineWidth) && pdfAnnotation is InkAnnotation && lineWidth != null) {
            pdfAnnotation.lineWidth = lineWidth.rounded(3)
        }
        if (changes.contains(PdfAnnotationChanges.color) && color != null) {
            val (color, isDark) = color
            val (_color, alpha, blendMode) = AnnotationColorGenerator.color(
                color,
                type = annotation.type,
                isDarkMode = isDark
            )
            pdfAnnotation.color = _color
            pdfAnnotation.alpha = alpha
            if (blendMode != null) {
                pdfAnnotation.blendMode = blendMode
            }
        }

        if (changes.contains(PdfAnnotationChanges.contents) && contents != null) {
            pdfAnnotation.contents = contents
        }

        if (changes.contains(PdfAnnotationChanges.fontSize) && fontSize != null) {
            val textAnnotation = pdfAnnotation as? FreeTextAnnotation
            if (textAnnotation != null) {
                textAnnotation.textSize = fontSize
            }
        }

        processAnnotationObserving(
            pdfAnnotation,
            PdfAnnotationChanges.stringValues(changes),
            PdfReaderNotification.PSPDFAnnotationChanged
        )
    }

    private fun update(
        pdfAnnotation: Annotation,
        annotation: PDFDatabaseAnnotation,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDarkMode: Boolean
    ) {
        val changes = mutableListOf<PdfAnnotationChanges>()

        if (pdfAnnotation.baseColor != annotation.color) {
            val hexColor = annotation.color
            val (color, alpha, blendMode) = AnnotationColorGenerator.color(
                colorHex = hexColor,
                type = annotation.type,
                isDarkMode = isDarkMode
            )
            pdfAnnotation.color = color
            pdfAnnotation.alpha = alpha
            if (blendMode != null) {
                pdfAnnotation.blendMode = blendMode
            }

            changes.add(PdfAnnotationChanges.color)
        }

        when (annotation.type) {
            org.zotero.android.database.objects.AnnotationType.highlight,
            org.zotero.android.database.objects.AnnotationType.underline -> {
                val newBoundingBox =
                    annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)
                if (newBoundingBox != pdfAnnotation.boundingBox.rounded(3)) {
                    pdfAnnotation.boundingBox = newBoundingBox
                    changes.add(PdfAnnotationChanges.boundingBox)

                    (pdfAnnotation as TextMarkupAnnotation).rects =
                        annotation.rects(boundingBoxConverter = annotationBoundingBoxConverter)
                    changes.add(PdfAnnotationChanges.rects)
                } else {
                    val newRects =
                        annotation.rects(boundingBoxConverter = annotationBoundingBoxConverter)
                    val oldRects =
                        ((pdfAnnotation as TextMarkupAnnotation).rects).map { it.rounded(3) }
                    if (newRects != oldRects) {
                        pdfAnnotation.rects = newRects
                        changes.add(PdfAnnotationChanges.rects)
                    }
                }
            }

            org.zotero.android.database.objects.AnnotationType.ink -> {
                val inkAnnotation = pdfAnnotation as? InkAnnotation
                if (inkAnnotation != null) {
                    val newPaths =
                        annotation.paths(boundingBoxConverter = annotationBoundingBoxConverter)
                    val oldPaths = (inkAnnotation.lines).map { points ->
                        points.map { it.rounded(3) }
                    }

                    if (newPaths != oldPaths) {
                        changes.add(PdfAnnotationChanges.paths)
                        inkAnnotation.lines = newPaths
                    }

                    val lineWidth = annotation.lineWidth
                    if (lineWidth != null && lineWidth != inkAnnotation.lineWidth) {
                        inkAnnotation.lineWidth = lineWidth
                        changes.add(PdfAnnotationChanges.lineWidth)
                    }
                }
            }

            org.zotero.android.database.objects.AnnotationType.image, org.zotero.android.database.objects.AnnotationType.text -> {
                val newBoundingBox =
                    annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)
                if (pdfAnnotation.boundingBox.rounded(3) != newBoundingBox) {
                    changes.add(PdfAnnotationChanges.boundingBox)
                    pdfAnnotation.boundingBox = newBoundingBox
                }
            }

            org.zotero.android.database.objects.AnnotationType.note -> {
                val newBoundingBox =
                    annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)
                val bb = pdfAnnotation.boundingBox
                if (bb.left.rounded(3) != newBoundingBox.left || bb.bottom.rounded(3) != newBoundingBox.bottom) {
                    changes.add(PdfAnnotationChanges.boundingBox)
                    pdfAnnotation.boundingBox = newBoundingBox
                }
            }
        }
//
        if (changes.isEmpty()) {
            return
        }

        annotationPreviewManager.store(
            annotation = pdfAnnotation,
            rawDocument = this.rawDocument,
            parentKey = parentKey,
            libraryId = libraryId,
            isDark = viewState.isDark,
            annotationMaxSideSize = annotationMaxSideSize
        )
        processAnnotationObserving(pdfAnnotation, PdfAnnotationChanges.stringValues(changes),  PdfReaderNotification.PSPDFAnnotationChanged)
    }

    private fun canUpdate(key: AnnotationKey, item: RItem, index: Int): Boolean {
        when (item.changeType) {
            UpdatableChangeType.sync.name ->
                return true

            UpdatableChangeType.syncResponse.name ->
                return false
        }

        if (!viewState.selectedAnnotationCommentActive || viewState.selectedAnnotationKey != key) {
            return true
        }

        val newComment =
            item.fields.where().key(FieldKeys.Item.Annotation.comment).findFirst()?.value
        val oldComment = this.databaseAnnotations!![index]!!.fields.where()
            .key(FieldKeys.Item.Annotation.comment).findFirst()?.value
        return oldComment == newComment
    }

    override fun selectAnnotation(key: AnnotationKey) {
        if (!viewState.sidebarEditingEnabled && key != viewState.selectedAnnotationKey) {
            _select(key = key, didSelectInDocument = false)
        }
    }


    fun selectAnnotationFromDocument(key: AnnotationKey) {
        if (!viewState.sidebarEditingEnabled) {
            _select(key = key, didSelectInDocument = true)
        }
    }

    private fun deselectSelectedAnnotation(annotation: Annotation) {
//        if (viewState.selectedAnnotationKey?.key == annotation.key ) {
//            _select(key = null, didSelectInDocument = false)
//        }
    }

    val selectedAnnotation: org.zotero.android.pdf.data.PDFAnnotation?
        get() {
            return viewState.selectedAnnotationKey?.let { annotation(it) }
        }

    override fun onCleared() {
        fragmentManager.commit(allowStateLoss = true) {
            remove(this@PdfReaderViewModel.pdfUiFragment)
        }
        pdfFragment.removeOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)

        EventBus.getDefault().unregister(this)
        liveAnnotations?.removeAllChangeListeners()
        annotationPreviewManager.deleteAll(
            parentKey = viewState.key,
            libraryId = viewState.library.identifier
        )
        thumbnailPreviewManager.deleteAll(
            key = viewState.key,
            libraryId = viewState.library.identifier
        )
        annotationPreviewManager.cancelProcessing()
        annotationPreviewFileCache.cancelProcessing()
        clearThumbnailCaches()
        document.annotationProvider
            .getAllAnnotationsOfTypeAsync(AnnotationsConfig.supported)
            .toList()
            .blockingGet()
            .forEach {
                this.document.annotationProvider.removeAnnotationFromPage(it)
            }
        submitPendingPage(pdfUiFragment.pageIndex)
        val activity = pdfUiFragment.activity
        if (activity != null) {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(
                WindowInsetsCompat.Type.systemBars()
            )
        }
        super.onCleared()
    }

    private fun clearThumbnailCaches() {
        thumbnailPreviewManager.cancelProcessing()
        thumbnailsPreviewFileCache.cancelProcessing()
        thumbnailPreviewMemoryCache.clear()
    }

    private fun generatePdfConfiguration(pdfSettings: PDFSettings): PdfActivityConfiguration {
        if (!PSPDFKitPreferences.get(context).isAnnotationCreatorSet) {
            PSPDFKitPreferences.get(context).setAnnotationCreator(viewState.displayName)
        }

        val scrollDirection = when (pdfSettings.direction) {
            PageScrollDirection.HORIZONTAL -> com.pspdfkit.configuration.page.PageScrollDirection.HORIZONTAL
            PageScrollDirection.VERTICAL -> com.pspdfkit.configuration.page.PageScrollDirection.VERTICAL
        }
        val pageMode = when (pdfSettings.pageMode) {
            PageLayoutMode.SINGLE -> com.pspdfkit.configuration.page.PageLayoutMode.SINGLE
            PageLayoutMode.DOUBLE -> com.pspdfkit.configuration.page.PageLayoutMode.DOUBLE
            PageLayoutMode.AUTOMATIC -> com.pspdfkit.configuration.page.PageLayoutMode.AUTO
        }
        val scrollMode = when (pdfSettings.transition) {
            org.zotero.android.pdf.data.PageScrollMode.JUMP -> PageScrollMode.PER_PAGE
            org.zotero.android.pdf.data.PageScrollMode.CONTINUOUS -> PageScrollMode.CONTINUOUS
        }
        val fitMode = when (pdfSettings.pageFitting) {
            PageFitting.FIT -> PageFitMode.FIT_TO_WIDTH
            PageFitting.FILL -> PageFitMode.FIT_TO_SCREEN
        }
        val isCalculatedThemeDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark
        val themeMode = when (isCalculatedThemeDark) {
            true -> ThemeMode.NIGHT
            false -> ThemeMode.DEFAULT
        }

        return PdfActivityConfiguration.Builder(context)
            .scrollDirection(scrollDirection)
            .scrollMode(scrollMode)
            .fitMode(fitMode)
            .layoutMode(pageMode)
            .invertColors(isCalculatedThemeDark)
            .themeMode(themeMode)
            .showNoteEditorForNewNoteAnnotations(false)
//            .disableFormEditing()
//            .disableAnnotationRotation()
//            .setSelectedAnnotationResizeEnabled(false)
            .autosaveEnabled(false)
            .scrollbarsEnabled(true)
            .disableDefaultToolbar()
            .hideDocumentTitleOverlay()
            .enableStylusOnDetection(true)
            .hideUserInterfaceWhenCreatingAnnotations(true)
            .build()
    }

    override fun loadAnnotationPreviews(keys: List<String>) {
        if (keys.isEmpty()) else {
            return
        }

        val isDark = viewState.isDark
        val libraryId = viewState.library.identifier

        for (key in keys) {
            if (annotationPreviewMemoryCache.getBitmap(key) != null) {
                continue
            }
            annotationPreviewFileCache.preview(
                key = key,
                parentKey = viewState.key,
                libraryId = libraryId,
                isDark = isDark
            )
        }
    }

    override fun onAnnotationSearch(text: String) {
        updateState {
            copy(annotationSearchTerm = text)
        }
        onAnnotationSearchStateFlow.tryEmit(text)
    }

    private fun searchAnnotations(term: String) {
        val trimmedTerm = term.trim().trim { it == '\n' }
        filterAnnotations(term = trimmedTerm, filter = viewState.filter)
    }

    private fun filterAnnotations(term: String, filter: AnnotationsFilter?) {
        if (term.isEmpty() && filter == null) {
            val snapshot = viewState.snapshotKeys ?: return

            this.document.annotationProvider.getAllAnnotationsOfType(
                EnumSet.allOf(
                    AnnotationType::class.java
                )
            ).forEach { annotation ->
                if (annotation.flags.contains(AnnotationFlags.HIDDEN)) {
                    annotation.flags =
                        EnumSet.copyOf(annotation.flags - AnnotationFlags.HIDDEN)
                    processAnnotationObserving(annotation, listOf("flags"),
                        PdfReaderNotification.PSPDFAnnotationChanged
                    )
                }
            }

            updateState {
                copy(
                    snapshotKeys = null,
                    sortedKeys = snapshot,
                    annotationSearchTerm = "",
                    filter = null
                )
            }
            return
        }

        val snapshot = viewState.snapshotKeys ?: viewState.sortedKeys
        val filteredKeys = filteredKeys(snapshot = snapshot, term = term, filter = filter)

        this.document.annotationProvider.getAllAnnotationsOfType(
            EnumSet.allOf(
                AnnotationType::class.java
            )
        ).forEach { annotation ->
            val isHidden =
                filteredKeys.firstOrNull { it.key == (annotation.key ?: annotation.uuid) } == null
            if (isHidden && !annotation.flags.contains(AnnotationFlags.HIDDEN)) {
                annotation.flags =
                    EnumSet.copyOf(annotation.flags + AnnotationFlags.HIDDEN)
                processAnnotationObserving(annotation, listOf("flags"),  PdfReaderNotification.PSPDFAnnotationChanged)
            } else if (!isHidden && annotation.flags.contains(AnnotationFlags.HIDDEN)) {
                annotation.flags =
                    EnumSet.copyOf(annotation.flags - AnnotationFlags.HIDDEN)
                processAnnotationObserving(annotation, listOf("flags"),  PdfReaderNotification.PSPDFAnnotationChanged)
            }
        }

        if (viewState.snapshotKeys == null) {
            updateState {
                copy(snapshotKeys = viewState.sortedKeys)
            }
        }
        updateState {
            copy(sortedKeys = filteredKeys, annotationSearchTerm = term, filter = filter)
        }
    }

    private fun filteredKeys(
        snapshot: List<AnnotationKey>,
        term: String,
        filter: AnnotationsFilter?
    ): List<AnnotationKey> {
        if (term.isEmpty() && filter == null) {
            return snapshot
        }
        return snapshot.filter { key ->
            val annotation = annotation(key) ?: return@filter false
            filter(
                annotation = annotation,
                term = term,
                displayName = viewState.displayName,
                username = viewState.username
            ) && filter(annotation = annotation, filter = filter)
        }
    }

    private fun filter(
        annotation: org.zotero.android.pdf.data.PDFAnnotation,
        term: String,
        displayName: String,
        username: String
    ): Boolean {
        if (term.isEmpty()) {
            return true
        }
        return annotation.key.lowercase() == term.lowercase() ||
                annotation.author(displayName = displayName, username = username)
                    .contains(term, ignoreCase = true) ||
                annotation.comment.contains(term, ignoreCase = true) ||
                (annotation.text ?: "").contains(term, ignoreCase = true) ||
                annotation.tags.any { it.name.contains(term, ignoreCase = true) }
    }

    private fun filter(
        annotation: org.zotero.android.pdf.data.PDFAnnotation,
        filter: AnnotationsFilter?
    ): Boolean {
        if (filter == null) {
            return true
        }
        val hasTag =
            if (filter.tags.isEmpty()) true else annotation.tags.firstOrNull {
                filter.tags.contains(
                    it.name
                )
            } != null
        val hasColor =
            if (filter.colors.isEmpty()) true else filter.colors.contains(annotation.color)
        return hasTag && hasColor
    }

    override fun showFilterPopup() {
        val colors = mutableSetOf<String>()
        val tags = mutableSetOf<Tag>()

        val processAnnotation: (org.zotero.android.pdf.data.PDFAnnotation) -> Unit = { annotation ->
            colors.add(annotation.color)
            for (tag in annotation.tags) {
                tags.add(tag)
            }
        }

        for (annotation in this.databaseAnnotations!!) {
            processAnnotation(PDFDatabaseAnnotation.init(item = annotation)!!)
        }
        for (annotation in this.viewState.pdfDocumentAnnotations.values) {
            processAnnotation(annotation)
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
        ScreenArguments.pdfFilterArgs = PdfFilterArgs(
            filter = viewState.filter,
            availableColors = sortedColors,
            availableTags = sortedTags
        )
        triggerEffect(PdfReaderViewEffect.ShowPdfFilters)
    }

    fun toggleSideBar() {
        updateState {
            copy(showSideBar = !showSideBar)
        }
    }

    private fun set(filter: AnnotationsFilter?) {
        if (filter == viewState.filter) {
            return
        }
        filterAnnotations(term = viewState.annotationSearchTerm, filter = filter)
    }

    fun navigateToPdfSettings() {
        ScreenArguments.pdfSettingsArgs = PdfSettingsArgs(defaults.getPDFSettings())
        triggerEffect(PdfReaderViewEffect.ShowPdfSettings)
    }

    override fun showToolOptions() {
        val tool = this.activeAnnotationTool ?: return

        val colorHex = this.toolColors[tool]
        var size: Float? = null
        when (tool) {
            AnnotationTool.INK -> {
                size = this.activeLineWidth
            }
            AnnotationTool.FREETEXT -> {
                size = this.activeFontSize
            }
            AnnotationTool.ERASER -> {
                size = this.activeEraserSize
            }
            else -> {
                size = null
            }
        }

        ScreenArguments.pdfReaderColorPickerArgs = PdfReaderColorPickerArgs(
            tool = tool,
            size = size,
            colorHex = colorHex,
        )
        triggerEffect(PdfReaderViewEffect.ShowPdfColorPicker)
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun onStop(isChangingConfigurations: Boolean) {
        pdfDocumentBeforeFragmentDestruction = pdfFragment.document!!
        if (isChangingConfigurations) {
            removeFragment()
        }
    }

    fun removeFragment() {
        fragmentManager.commit {
            remove(this@PdfReaderViewModel.pdfUiFragment)
        }
    }

    private fun replaceFragment() {
        val updatedConfiguration = generatePdfConfiguration(defaults.getPDFSettings())

        this.pdfUiFragment =
            PdfUiFragmentBuilder
                .fromUri(context, this.uri)
                .fragmentClass(CustomPdfUiFragment::class.java)
                .configuration(updatedConfiguration)
                .build()

        this@PdfReaderViewModel.pdfUiFragment.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                this@PdfReaderViewModel.pdfFragment = pdfUiFragment.pdfFragment!!
                this@PdfReaderViewModel.pdfFragment.addDrawableProvider(searchResultHighlighter)
                addDocumentListener2()
                addOnAnnotationCreationModeChangeListener()
                setOnPreparePopupToolbarListener()
//                updateVisibilityOfAnnotations()

            }

            override fun onDestroy(owner: LifecycleOwner) {
                pdfUiFragment.lifecycle.removeObserver(this)
            }
        })


        fragmentManager.commit {
            replace(containerId, this@PdfReaderViewModel.pdfUiFragment)
        }
    }

    private fun addDocumentListener2() {
        this.pdfFragment.addDocumentListener(object : DocumentListener {
            override fun onDocumentLoaded(document: PdfDocument) {
                this@PdfReaderViewModel.onDocumentLoaded(document)

                if (queuedUpPdfReaderColorPickerResult != null) {
                    setToolOptions(
                        hex = queuedUpPdfReaderColorPickerResult!!.colorHex,
                        size = queuedUpPdfReaderColorPickerResult!!.size,
                        tool = queuedUpPdfReaderColorPickerResult!!.annotationTool
                    )
                    queuedUpPdfReaderColorPickerResult = null

                }
            }

            override fun onPageClick(
                document: PdfDocument,
                pageIndex: Int,
                event: MotionEvent?,
                pagePosition: PointF?,
                clickedAnnotation: Annotation?
            ): Boolean {
                return false
            }
        })
    }

    private fun addOnAnnotationCreationModeChangeListener() {
        this.pdfFragment.addOnAnnotationCreationModeChangeListener(object :
            AnnotationManager.OnAnnotationCreationModeChangeListener {
            override fun onEnterAnnotationCreationMode(p0: AnnotationCreationController) {
                hidePspdfkitToolbars()
                set(true)
            }

            override fun onChangeAnnotationCreationMode(p0: AnnotationCreationController) {
                set(true)
            }

            override fun onExitAnnotationCreationMode(p0: AnnotationCreationController) {
                set(false)
            }

        })
    }

    private fun toggleTopAndBottomBarVisibility(isVisible: Boolean) {
        updateState {
            copy(isTopBarVisible = isVisible)
        }
    }

    private fun updateVisibilityOfAnnotations() {
        handler.postDelayed({
            val pageIndex = if (pdfUiFragment.pageIndex == -1) 0 else pdfUiFragment.pageIndex
            this.document.annotationProvider.getAnnotations(pageIndex).forEach {
                this.pdfFragment.notifyAnnotationHasChanged(it)
            }
        }, 200)
    }

    fun toggleToolbarButton() {
        updateState {
            copy(showCreationToolbar = !viewState.showCreationToolbar)
        }
        if (!viewState.showCreationToolbar) {
            pdfFragment.exitCurrentlyActiveMode()
        }
    }
    override fun toggle(tool: AnnotationTool) {
        val color = this.toolColors[tool]
        toggle(annotationTool = tool, color = color)
    }

    fun toggle(annotationTool: AnnotationTool, color: String?) {
        val tool = pdfFragment.activeAnnotationTool

        if (tool != null && tool != AnnotationTool.ERASER && tool != this.toolHistory.lastOrNull()) {
            this.toolHistory.add(tool)
            if (this.toolHistory.size > 2) {
                this.toolHistory.removeAt(0)
            }
        }

        if (pdfFragment.activeAnnotationTool == annotationTool) {
            pdfFragment.exitCurrentlyActiveMode()
            return
        }

//        fragment.enterAnnotationCreationMode(annotationTool)

        var drawColor: Int? = null
        var blendMode: BlendMode? = null

        if (color != null) {
            val type: org.zotero.android.database.objects.AnnotationType?

            when(annotationTool) {
                AnnotationTool.HIGHLIGHT -> {
                    type = org.zotero.android.database.objects.AnnotationType.highlight
                }
                AnnotationTool.UNDERLINE -> {
                    type = org.zotero.android.database.objects.AnnotationType.underline
                }
                else -> {
                    type = null
                }
            }
            val (_color, _, bM) = AnnotationColorGenerator.color(
                colorHex = color,
                type = type,
                isDarkMode = viewState.isDark
            )
            drawColor = _color
            blendMode = bM ?: BlendMode.NORMAL
        }

        updateAnnotationToolDrawColorAndSize(annotationTool, drawColor)

    }

    private fun updateAnnotationToolDrawColorAndSize(
        annotationTool: AnnotationTool,
        drawColor: Int?
    ) {
        pdfFragment.exitCurrentlyActiveMode()
        when (annotationTool) {
            AnnotationTool.INK -> {
                configureInk(drawColor, this.activeLineWidth)
            }

            AnnotationTool.FREETEXT -> {
                configureFreeText(drawColor, this.activeFontSize)
            }

            AnnotationTool.HIGHLIGHT -> {
                configureHighlight(drawColor)
            }

            AnnotationTool.NOTE -> {
                configureNote(drawColor)
            }

            AnnotationTool.SQUARE -> {
                configureSquare(drawColor)
            }

            AnnotationTool.UNDERLINE -> {
                configureUnderline(drawColor)
            }

            AnnotationTool.ERASER -> {
                configureEraser(this.activeEraserSize)
            }

            else -> {}
        }
        pdfFragment.enterAnnotationCreationMode(annotationTool)
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    private fun configureNote(drawColor: Int?) {
        if (drawColor == null) {
            return
        }
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.NOTE,
                NoteAnnotationConfiguration.builder(context)
                    .setDefaultColor(drawColor)
                    .build()
            )
    }

    private fun configureUnderline(drawColor: Int?) {
        if (drawColor == null) {
            return
        }
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.UNDERLINE,
                NoteAnnotationConfiguration.builder(context)
                    .setDefaultColor(drawColor)
                    .build()
            )
    }

    private fun configureEraser(activeEraserSize: Float) {
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.ERASER,
                EraserToolConfiguration.builder()
                    .setDefaultThickness(activeEraserSize)
                    .build()
            )

    }

    private fun configureHighlight(drawColor: Int?) {
        if (drawColor == null) {
            return
        }
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.HIGHLIGHT,
                MarkupAnnotationConfiguration.builder(context, AnnotationTool.HIGHLIGHT) // Configure which color is used when creating ink annotations.
                    .setDefaultColor(drawColor)
                    .build()
            )

    }

    private fun configureFreeText(drawColor: Int?, textSize: Float) {
        if (drawColor == null) {
            return
        }
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.FREETEXT,
                FreeTextAnnotationConfiguration.builder(context)
                    .setDefaultColor(drawColor)
                    .setDefaultTextSize(textSize)
                    .build()
            )

    }

    private fun configureInk(drawColor: Int?, activeLineWidth: Float) {
        if (drawColor == null) {
            return
        }
        pdfFragment.annotationConfiguration
            .put(
                AnnotationTool.INK,
                InkAnnotationConfiguration.builder(context) // Configure which color is used when creating ink annotations.
                    .setDefaultColor(drawColor)
                    .setDefaultThickness(activeLineWidth)
                    .build()
            )

    }

    private fun configureSquare(drawColor: Int?) {
        if (drawColor == null) {
            return
        }

        // Annotation configuration can be configured through PdfFragment for each annotation type.
        pdfFragment.annotationConfiguration
            .put(
                AnnotationType.SQUARE,
                ShapeAnnotationConfiguration.builder(context, AnnotationType.SQUARE)
                    .setDefaultColor(drawColor)
                    .build()
            )
    }

    override val activeAnnotationTool: AnnotationTool? get() {
        return this.pdfFragment.activeAnnotationTool
    }

    override fun canUndo() : Boolean {
        return this.pdfFragment.undoManager.canUndo()
    }

    override fun canRedo() : Boolean {
        return this.pdfFragment.undoManager.canRedo()
    }

    override fun onUndoClick() {
        this.pdfFragment.undoManager.undo()
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    override fun onRedoClick() {
        this.pdfFragment.undoManager.redo()
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    override fun onCloseClick() {
        toggleToolbarButton()
    }

    private fun add(annotations: List<Annotation>) {
        val (keptAsIs, toRemove, toAdd) = transformIfNeeded(annotations = annotations)
        val finalAnnotations = keptAsIs + toAdd
        for (annotation in finalAnnotations) {
            if (annotation.key == null) {
                annotation.creator = viewState.displayName
                annotation.customData =
                    JSONObject().put(AnnotationsConfig.keyKey, KeyGenerator.newKey())
            }
        }

        if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
            toRemove.forEach {
                this.document.annotationProvider.removeAnnotationFromPage(it)
            }
            finalAnnotations.forEach {
                this.document.annotationProvider.addAnnotationToPage(it)
            }
        }

        if (finalAnnotations.isEmpty()) {
            return
        }

        val documentAnnotations = finalAnnotations.mapNotNull { annotation ->
            val documentAnnotation = AnnotationConverter.annotation(
                this.document,
                annotation,
                color = annotation.baseColor,
                username = viewState.username,
                displayName = viewState.displayName,
                boundingBoxConverter = this.annotationBoundingBoxConverter
            ) ?: return@mapNotNull null

            this.annotationPreviewManager.store(
                this.rawDocument,
                annotation,
                parentKey = viewState.key,
                libraryId = viewState.library.identifier,
                isDark = viewState.isDark,
                annotationMaxSideSize = annotationMaxSideSize
            )

            documentAnnotation
        }

        documentAnnotations.map { it.page }.toSet().forEach { pageIndex ->
            thumbnailPreviewManager.store(
                pageIndex = pageIndex,
                key = viewState.key,
                document = this.document,
                libraryId = viewState.library.identifier,
                isDark = viewState.isDark,
            )
        }
        val request = CreatePDFAnnotationsDbRequest(
            attachmentKey = viewState.key,
            libraryId = viewState.library.identifier,
            annotations = documentAnnotations,
            userId = viewState.userId,
            schemaController = this.schemaController,
            boundingBoxConverter = this.annotationBoundingBoxConverter
        )
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel: can't add annotations")
                return@launch
            }
        }

    }

    private fun tool(annotation: Annotation): AnnotationTool? {
        return when (annotation) {
            is HighlightAnnotation -> {
                AnnotationTool.HIGHLIGHT
            }
            is NoteAnnotation -> {
                AnnotationTool.NOTE
            }
            is SquareAnnotation -> {
                AnnotationTool.SQUARE
            }
            is InkAnnotation -> {
                AnnotationTool.INK
            }
            is FreeTextAnnotation -> {
                AnnotationTool.FREETEXT
            }
            is UnderlineAnnotation -> {
                AnnotationTool.UNDERLINE
            }
            else -> {
                null
            }
        }
    }

    private fun transformIfNeeded(annotations: List<Annotation>): Triple<List<Annotation>, List<Annotation>, List<Annotation>> {
        val keptAsIs = mutableListOf<Annotation>()
        val toRemove = mutableListOf<Annotation>()
        val toAdd = mutableListOf<Annotation>()

        for (annotation in annotations) {
            val tool = tool(annotation) ?:continue
            val activeColor = this.toolColors[tool] ?: continue
            val activeColorString = activeColor
            val (_, _, blendMode) = AnnotationColorGenerator.color(
                activeColor,
                type = annotation.type.zoteroType(),
                isDarkMode = viewState.isDark
            )
            annotation.blendMode = blendMode ?: BlendMode.NORMAL

            if (annotation.key == null || annotation(AnnotationKey(key = annotation.key!!, type = AnnotationKey.Kind.database)) == null) {
            } else {
                keptAsIs.add(annotation)
                continue
            }

            val splitAnnotations = splitIfNeeded(a = annotation)

            if (splitAnnotations.size <= 1) {
                keptAsIs.add(annotation)
                continue
            }
            Timber.i("PdfReaderViewModel: did split annotations into ${splitAnnotations.size}")
            toRemove.add(annotation)
            toAdd.addAll(splitAnnotations)
        }
        return Triple(keptAsIs, toRemove, toAdd)
    }

    private fun createHighlightOrUnderlineAnnotations(
        isHighlight: Boolean,
        splitRects: List<List<RectF>>,
        original: TextMarkupAnnotation,
    ): List<TextMarkupAnnotation> {
        if (splitRects.size <= 1) {
            return listOf(original)
        }
        return splitRects.map { rects ->
            val new = if (isHighlight) HighlightAnnotation(
                original.pageIndex,
                rects
            ) else UnderlineAnnotation(original.pageIndex, rects)
            new.boundingBox = AnnotationBoundingBoxCalculator.boundingBox(rects)
            new.alpha = original.alpha
            new.color = original.color
            new.blendMode = original.blendMode
            new.contents = original.contents
            new.customData = JSONObject().put(AnnotationsConfig.keyKey, KeyGenerator.newKey())
            new
        }
    }

    private fun createInkAnnotations(splitPaths: List<List<List<PointF>>>, original: InkAnnotation): List<InkAnnotation> {
        if (splitPaths.size <= 1) {
            return listOf(original)
        }
        return splitPaths.map { paths ->
            val new = InkAnnotation(original.pageIndex)
            new.lines = paths
            new.lineWidth = original.lineWidth
            new.alpha = original.alpha
            new.color = original.color
            new.blendMode = original.blendMode
            new.contents = original.contents
            new.customData = JSONObject().put(AnnotationsConfig.keyKey, KeyGenerator.newKey())
            new
        }
    }

    private fun splitIfNeeded(a: Annotation): List<Annotation> {
        if (a is HighlightAnnotation || a is UnderlineAnnotation ) {
            a as TextMarkupAnnotation
            val isHighlightAnnotation = a is HighlightAnnotation
            val rects = a.rects
            val splitRects = AnnotationSplitter.splitRectsIfNeeded(rects = rects)
            if (splitRects != null) {
                return createHighlightOrUnderlineAnnotations(
                    isHighlight = isHighlightAnnotation,
                    splitRects = splitRects,
                    original = a
                )
            }
        }
        val inkAnnotation = a as? InkAnnotation
        if (inkAnnotation != null) {
            val paths = inkAnnotation.lines
            val splitPaths = AnnotationSplitter.splitPathsIfNeeded(paths = paths)
            if (splitPaths != null) {
                return createInkAnnotations(splitPaths, original = inkAnnotation)
            }
        }

        return listOf(a)
    }

    private fun remove(key: AnnotationKey) {
        val annotation = annotation(key) ?: return
        val pdfAnnotation = this.document.annotationProvider.getAnnotations(annotation.page)
            .firstOrNull { it.key == annotation.key } ?: return
        remove(annotations = listOf(pdfAnnotation))
    }

    private fun remove(annotations: List<Annotation>) {
        val keys = annotations.mapNotNull { it.key }

        for (annotation in annotations) {
            annotationPreviewManager.delete(annotation, parentKey = viewState.key, libraryId = viewState.library.identifier)
        }

        if(keys.isEmpty()) { return }

        val request = MarkObjectsAsDeletedDbRequest(clazz = RItem::class, keys = keys, libraryId = viewState.library.identifier)

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel: can't remove annotations $keys")
                return@launch
            }
        }
    }

    private fun updateChangedColorForTool() {
        val tool = this.changedColorForTool
        val color = this.toolColors[tool]
    }

    private fun setToolOptions(hex: String?, size: Float?, tool: AnnotationTool) {
        if (hex != null) {
            when (tool) {
                AnnotationTool.HIGHLIGHT -> {
                    defaults.setHighlightColorHex(hex)
                }
                AnnotationTool.NOTE -> {
                    defaults.setNoteColorHex(hex)
                }
                AnnotationTool.SQUARE -> {
                    defaults.setSquareColorHex(hex)
                }
                AnnotationTool.INK -> {
                    defaults.setInkColorHex(hex)
                }
                AnnotationTool.FREETEXT -> {
                    defaults.setTextColorHex(hex)
                }
                AnnotationTool.UNDERLINE -> {
                    defaults.setUnderlineColorHex(hex)
                }
                else -> {
                    //no-op
                }
            }
        }
        if (size != null) {
            when (tool) {
                AnnotationTool.ERASER -> {
                    defaults.setActiveEraserSize(size)
                }
                AnnotationTool.INK -> {
                    defaults.setActiveLineWidth(size)
                }
                AnnotationTool.FREETEXT -> {
                    defaults.setActiveFontSize(size)
                }
                else -> {
                    //no-op
                }
            }
        }
        if (hex != null) {
            this.toolColors[tool] = hex
            this.changedColorForTool = tool
            updateChangedColorForTool()
        }
        if (size != null) {
            when (tool) {
                AnnotationTool.INK -> {
                    this.activeLineWidth = size
                }
                AnnotationTool.ERASER -> {
                    this.activeEraserSize = size
                }
                AnnotationTool.FREETEXT -> {
                    this.activeFontSize = size
                }
                else -> {
                    //no-op
                }
            }
        }
        var drawColor: Int? = null
        if (hex != null) {
            val type: org.zotero.android.database.objects.AnnotationType?

            when(tool) {
                AnnotationTool.HIGHLIGHT -> {
                    type = org.zotero.android.database.objects.AnnotationType.highlight
                }
                AnnotationTool.UNDERLINE -> {
                    type = org.zotero.android.database.objects.AnnotationType.underline
                }
                else -> {
                    type = null
                }
            }

            val (_color, _, bM) = AnnotationColorGenerator.color(
                colorHex = hex,
                type = type,
                isDarkMode = viewState.isDark
            )
            drawColor = _color
        }
        updateAnnotationToolDrawColorAndSize(tool, drawColor = drawColor)
    }

    override fun onCommentTextChange(annotationKey: String, comment: String) {
        updateState {
            copy(commentFocusText = comment)
        }
        onCommentChangeFlow.tryEmit(annotationKey to comment)
    }

    fun setComment(annotationKey: String, comment: String) {
        set(comment = comment, key = annotationKey)
    }

    private fun set(comment: String, key: String) {
        val annotation = annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return

        val htmlComment = comment //TODO Use HtmlAttributedStringConverter

        this.comments[key] = comment

        update(annotation = annotation, contents = htmlComment, document = this.document)
    }

    override fun onCommentFocusFieldChange(annotationKey: String) {
        val key = AnnotationKey(key = annotationKey, type = AnnotationKey.Kind.database)
        val annotation =
            annotation(key)
                ?: return
        selectAnnotationFromDocument(key)

        updateState {
            copy(
                commentFocusKey = annotationKey,
                commentFocusText = annotation.comment
            )
        }
    }

    override fun onTagsClicked(annotation: org.zotero.android.pdf.data.PDFAnnotation) {
//        if (!annotation.isAuthor(viewState.userId)) {
//            return
//        }
        val annotationKey = AnnotationKey(key = annotation.key, type = AnnotationKey.Kind.database)
        selectAnnotationFromDocument(annotationKey)

        val selected = annotation.tags.map { it.name }.toSet()

        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = viewState.library.identifier,
            selectedTags = selected,
            tags = emptyList(),
            callPoint = TagPickerResult.CallPoint.PdfReaderScreen,
        )

        triggerEffect(PdfReaderViewEffect.NavigateToTagPickerScreen)
    }

    private fun set(tags: List<Tag>, key: String) {
        val request = EditTagsForItemDbRequest(key = key, libraryId = viewState.library.identifier, tags = tags)
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel: can't set tags $key")
                return@launch
            }
        }
    }

    private fun setLineWidth(key:String, width: Float) {
        set(lineWidth = width, key = key)
    }

    private fun set(lineWidth: Float, key: String) {
        val annotation = annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return
        update(annotation = annotation, lineWidth = lineWidth, document = this.document)
    }

    override fun onMoreOptionsForItemClicked() {
        ScreenArguments.pdfAnnotationMoreArgs = PdfAnnotationMoreArgs(
            selectedAnnotation = selectedAnnotation,
            userId = viewState.userId,
            library = viewState.library
        )
        triggerEffect(PdfReaderViewEffect.ShowPdfAnnotationMore)
    }

    private fun set(
        color: String,
        lineWidth: Float,
        pageLabel: String,
        fontSize: Float,
        updateSubsequentLabels: Boolean,
        text: String,
        key: String
    ) {
        val annotation =
            annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return
        update(
            annotation = annotation,
            color = color to viewState.isDark,
            fontSize = fontSize,
            lineWidth = lineWidth,
            document = this.document
        )

        val values = mapOf(
            KeyBaseKeyPair(
                key = FieldKeys.Item.Annotation.pageLabel,
                baseKey = null
            ) to pageLabel,
            KeyBaseKeyPair(key = FieldKeys.Item.Annotation.text, baseKey = null) to text
        )
        val request = EditItemFieldsDbRequest(
            key = key,
            libraryId = viewState.library.identifier,
            fieldValues = values,
            dateParser = this.dateParser
        )

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel:  can't update annotation $key")
                return@launch
            }
        }
    }

    private fun submitPendingPage(page: Int) {
        store(page = page)
    }

    private fun store(page: Int) {
        val request = StorePageForItemDbRequest(
            key = viewState.key,
            libraryId = viewState.library.identifier,
            page = "$page"
        )
        ZoteroApplication.instance.applicationScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel: can't store page")
                return@launch
            }
        }
    }

    override fun setSidebarSliderSelectedOption(optionOrdinal: Int) {
        val option = PdfReaderSliderOptions.entries[optionOrdinal]
        updateState {
            copy(sidebarSliderSelectedOption = option)
        }
    }

    private fun createSnapshot(search: String) {
        val snapshot = mutableListOf<PdfReaderOutlineOptionsWithChildren>()
        append(outlines = document.outline, parent = null, snapshot = snapshot, search = search)
        if (snapshot.size == 1) {
            updateState {
                copy(
                    outlineSnapshot = snapshot,
                    outlineExpandedNodes = setOf(snapshot[0].outline.id)
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
        outlines: List<OutlineElement>,
        parent: PdfReaderOutlineOptionsWithChildren?,
        snapshot: MutableList<PdfReaderOutlineOptionsWithChildren>,
        search: String
    ) {
        val rows = mutableListOf<PdfReaderOutlineOptionsWithChildren>()
        for (element in outlines) {
            if (search.isEmpty()) {
                val outline = PdfReaderOutlineOptionsWithChildren(Outline(element = element, isActive = true))
                rows.add(outline)
                continue
            }

            val elementContainsSearch = outline(element, search)
            val childContainsSearch = child(element.children, search)

            if (!elementContainsSearch && !childContainsSearch) { continue }

            val outline = PdfReaderOutlineOptionsWithChildren(Outline(element = element, isActive = elementContainsSearch))
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
                append(outlines=  children, parent = rows[idx], snapshot = snapshot, search =  search)
                continue
            }

            val index = rows.indexOfFirst { row ->
                val pageIndex: Int? = (element.action as? GoToAction)?.pageIndex
                row.outline.title == element.title && row.outline.page == pageIndex

            }
            if (index == -1) {
                continue
            }

            append(outlines = children, parent = rows[index], snapshot = snapshot, search = search)
        }

    }

    private fun child(children: List<OutlineElement>, string: String): Boolean {
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

    private fun outline(outline: OutlineElement, string: String): Boolean {
        val pageIndex: Int? = (outline.action as? GoToAction)?.pageIndex
        return (outline.title ?: "").contains(string, ignoreCase = true) || string.toIntOrNull() == pageIndex
    }

    override fun onOutlineSearch(search: String) {
        if (search == viewState.outlineSearchTerm) {
            return
        }
        updateState {
            copy(outlineSearchTerm = search)
        }
        onOutlineSearchStateFlow.tryEmit(search)
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

    private fun searchOutlines(search: String) {
        createSnapshot(search = search)
    }

    override fun onOutlineItemTapped(outline: Outline) {
        if (!outline.isActive) {
            return
        }
        focus(page = outline.page)
        if (!isTablet) {
            toggleSideBar()
        }
    }

    override fun onOutlineItemChevronTapped(outline: Outline) {
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

    override fun selectThumbnail(row: PdfReaderThumbnailRow) {
        updateState {
            copy(selectedThumbnail = row)
        }
        scrollIfNeeded(row.pageIndex, false) {}
    }

    override fun loadThumbnailPreviews(pageIndex: Int) {
        val isDark = viewState.isDark
        val libraryId = viewState.library.identifier

        if (thumbnailPreviewMemoryCache.getBitmap(pageIndex) != null) {
            return
        }
        if (thumbnailPreviewManager.hasThumbnail(
                page = pageIndex,
                key = viewState.key,
                libraryId = libraryId,
                isDark = isDark
            )
        ) {
            thumbnailsPreviewFileCache.preview(
                pageIndex = pageIndex,
                key = viewState.key,
                libraryId = libraryId,
                isDark = isDark
            )
        } else {
            thumbnailPreviewManager.store(
                pageIndex = pageIndex,
                key = viewState.key,
                document = this.document,
                libraryId = viewState.library.identifier,
                isDark = viewState.isDark,
            )
        }

    }

    override fun togglePdfSearch() {
        ScreenArguments.pdfReaderSearchArgs = PdfReaderSearchArgs(
            pdfDocument = this.document,
            configuration = pdfFragment.configuration
        )
        updateState {
            copy(showPdfSearch = !showPdfSearch)
        }

    }

    override fun hidePdfSearch() {
        updateState {
            copy(
                showPdfSearch = false
            )
        }
    }
}

data class PdfReaderViewState(
    val key: String = "",
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val userId: Long = -1L,
    val username: String = "",
    val displayName: String = "",
    val selectedAnnotationKey: AnnotationKey? = null,
    val isDark: Boolean = false,
    val initialPage: Int? = null,
    val visiblePage: Int = 0,
    val focusSidebarKey: AnnotationKey? = null,
    val focusDocumentLocation: Pair<Int, RectF>? = null,
    val pdfDocumentAnnotations: Map<String, PDFDocumentAnnotation> = emptyMap(),
    val sortedKeys: List<AnnotationKey> = emptyList(),
    val snapshotKeys: List<AnnotationKey>? = null,
    var selectedAnnotationCommentActive: Boolean = false,
    val sidebarEditingEnabled: Boolean = false,
    val updatedAnnotationKeys: List<AnnotationKey>? = null,
    val annotationSearchTerm: String = "",
    val filter: AnnotationsFilter? = null,
    val showSideBar: Boolean = false,
    val showCreationToolbar: Boolean = false,
    val isColorPickerButtonVisible: Boolean = false,
    val commentFocusKey: String? = null,
    val commentFocusText: String = "",
    val isTopBarVisible: Boolean = true,
    val sidebarSliderSelectedOption: PdfReaderSliderOptions = PdfReaderSliderOptions.Annotations,
    val outlineExpandedNodes: Set<String> = emptySet(),
    val outlineSnapshot: List<PdfReaderOutlineOptionsWithChildren> = emptyList(),
    val outlineSearchTerm: String = "",
    val isOutlineEmpty: Boolean = false,
    val thumbnailRows: ImmutableList<PdfReaderThumbnailRow> = persistentListOf(),
    val selectedThumbnail: PdfReaderThumbnailRow? = null,
    val showPdfSearch: Boolean = false,
) : ViewState {

    fun isAnnotationSelected(annotationKey: String): Boolean {
        return this.selectedAnnotationKey?.key == annotationKey
    }

    fun isOutlineSectionCollapsed(id: String): Boolean {
        val isCollapsed = !outlineExpandedNodes.contains(id)
        return isCollapsed
    }

    fun isThumbnailSelected(row: PdfReaderThumbnailRow): Boolean {
        return this.selectedThumbnail == row
    }
}

sealed class PdfReaderViewEffect : ViewEffect {
    object NavigateBack : PdfReaderViewEffect()
    object ShowPdfFilters : PdfReaderViewEffect()
    object ShowPdfSettings : PdfReaderViewEffect()
    object ShowPdfAnnotationMore: PdfReaderViewEffect()
    object ShowPdfColorPicker: PdfReaderViewEffect()
    data class ShowPdfAnnotationAndUpdateAnnotationsList(val scrollToIndex: Int, val showAnnotationPopup: Boolean): PdfReaderViewEffect()
    object ScreenRefresh: PdfReaderViewEffect()
    object ClearFocus: PdfReaderViewEffect()
    object NavigateToTagPickerScreen: PdfReaderViewEffect()
    data class ScrollThumbnailListToIndex(val scrollToIndex: Int): PdfReaderViewEffect()
}

data class AnnotationKey(
    val key: String,
    val type: Kind,
)  {
    enum class Kind {
        database,
        document,
    }

    val id: String get() {
        return this.key
    }
}
