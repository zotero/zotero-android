package org.zotero.android.pdf.reader

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Handler
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.BlendMode
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.configuration.EraserToolConfiguration
import com.pspdfkit.annotations.configuration.InkAnnotationConfiguration
import com.pspdfkit.annotations.configuration.MarkupAnnotationConfiguration
import com.pspdfkit.annotations.configuration.NoteAnnotationConfiguration
import com.pspdfkit.annotations.configuration.ShapeAnnotationConfiguration
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.special_mode.controller.AnnotationCreationController
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.internal.toHexString
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import org.zotero.android.api.network.CustomResult
import org.zotero.android.api.pojo.sync.KeyBaseKeyPair
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.EventBusConstants
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.database.requests.CreateAnnotationsDbRequest
import org.zotero.android.database.requests.EditAnnotationPathsDbRequest
import org.zotero.android.database.requests.EditAnnotationRectsDbRequest
import org.zotero.android.database.requests.EditItemFieldsDbRequest
import org.zotero.android.database.requests.EditTagsForItemDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.ktx.annotation
import org.zotero.android.ktx.baseColor
import org.zotero.android.ktx.index
import org.zotero.android.ktx.isZoteroAnnotation
import org.zotero.android.ktx.key
import org.zotero.android.ktx.rounded
import org.zotero.android.pdf.annotation.data.PdfAnnotationArgs
import org.zotero.android.pdf.annotation.data.PdfAnnotationColorResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationCommentResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationDeleteResult
import org.zotero.android.pdf.annotation.data.PdfAnnotationSizeResult
import org.zotero.android.pdf.cache.AnnotationPreviewCacheUpdatedEventStream
import org.zotero.android.pdf.cache.AnnotationPreviewFileCache
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerArgs
import org.zotero.android.pdf.colorpicker.data.PdfReaderColorPickerResult
import org.zotero.android.pdf.colorpicker.queuedUpPdfReaderColorPickerResult
import org.zotero.android.pdf.data.AnnotationBoundingBoxConverter
import org.zotero.android.pdf.data.AnnotationPreviewManager
import org.zotero.android.pdf.data.AnnotationsFilter
import org.zotero.android.pdf.data.DatabaseAnnotation
import org.zotero.android.pdf.data.DocumentAnnotation
import org.zotero.android.pdf.data.PDFSettings
import org.zotero.android.pdf.data.PageFitting
import org.zotero.android.pdf.data.PageLayoutMode
import org.zotero.android.pdf.data.PageScrollDirection
import org.zotero.android.pdf.data.PdfAnnotationChanges
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
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
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.SessionDataEventStream
import org.zotero.android.sync.Tag
import timber.log.Timber
import java.util.EnumSet
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val sessionDataEventStream: SessionDataEventStream,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val annotationPreviewManager: AnnotationPreviewManager,
    private val fileCache: AnnotationPreviewFileCache,
    private val context: Context,
    private val annotationPreviewCacheUpdatedEventStream: AnnotationPreviewCacheUpdatedEventStream,
    val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache,
    private val schemaController: SchemaController,
) : BaseViewModel2<PdfReaderViewState, PdfReaderViewEffect>(PdfReaderViewState()) {

    private var liveAnnotations: RealmResults<RItem>? = null
    private var databaseAnnotations: RealmResults<RItem>? = null
    private lateinit var annotationBoundingBoxConverter: AnnotationBoundingBoxConverter
    private var containerId = 0
    private lateinit var fragment: PdfFragment
    private var onAnnotationUpdatedListener: AnnotationProvider.OnAnnotationUpdatedListener? = null
    private lateinit var document: PdfDocument
    private lateinit var rawDocument: PdfDocument
    var comments = mutableMapOf<String, String>()
    private val onSearchStateFlow = MutableStateFlow("")
    private val onCommentChangeFlow = MutableStateFlow<Pair<String, String>?>(null)
    private lateinit var fragmentManager: FragmentManager
    private var isTablet: Boolean = false

    private val handler = Handler(context.mainLooper)

    //Used to recreate a new fragment preserving viewport state
    private lateinit var pdfDocumentBeforeFragmentDestruction: PdfDocument

    var annotationMaxSideSize = 0

    var toolColors: MutableMap<AnnotationTool, String> = mutableMapOf()
    var changedColorForTool: AnnotationTool? = null
    var activeLineWidth: Float = 0.0f
    var activeEraserSize: Float = 0.0f

    private var toolHistory = mutableListOf<AnnotationTool>()

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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: PdfAnnotationSizeResult) {
        setLineWidth(key = result.key, width = result.size)
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

    private fun update(pdfSettings: PDFSettings) {
        defaults.setPDFSettings(pdfSettings)
        pdfReaderThemeDecider.setPdfPageAppearanceMode(pdfSettings.appearanceMode)
        if (isTablet) {
            pdfDocumentBeforeFragmentDestruction = fragment.document!!
            replaceFragment()
        }
    }

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

    fun init(
        uri: Uri,
        annotationMaxSideSize: Int,
        containerId: Int,
        fragmentManager: FragmentManager,
        isTablet: Boolean,
    ) {
        this.isTablet = isTablet
        this.fragmentManager = fragmentManager
        this.containerId = containerId
        this.annotationMaxSideSize = annotationMaxSideSize

        if (this::fragment.isInitialized) {
            replaceFragment()
            return
        }

        EventBus.getDefault().register(this)

        initState()
        startObservingTheme()
        setupAnnotationCacheUpdateStream()
        setupSearchStateFlow()
        setupCommentChangeFlow()

        val pdfSettings = defaults.getPDFSettings()
        pdfReaderThemeDecider.setPdfPageAppearanceMode(pdfSettings.appearanceMode)
        val configuration = generatePdfConfiguration(pdfSettings)
        this@PdfReaderViewModel.fragment = PdfFragment.newInstance(uri, configuration)
        this@PdfReaderViewModel.fragment.addDocumentListener(object : DocumentListener {
            override fun onDocumentLoaded(document: PdfDocument) {
                this@PdfReaderViewModel.onDocumentLoaded(document)
            }
        })
        this.fragment.addOnAnnotationCreationModeChangeListener(object:
            AnnotationManager.OnAnnotationCreationModeChangeListener {
            override fun onEnterAnnotationCreationMode(p0: AnnotationCreationController) {
                set(true)
            }

            override fun onChangeAnnotationCreationMode(p0: AnnotationCreationController) {
                set(true)
            }

            override fun onExitAnnotationCreationMode(p0: AnnotationCreationController) {
                set(false)
            }

        })
        fragmentManager.commit {
            add(containerId, this@PdfReaderViewModel.fragment)
        }
    }

    private fun setColor(key: String, color: String) {
        setC(key = key, color = color)
    }

    private fun setC(color: String, key:String) {
        val annotation = annotation(AnnotationKey(key = key, type = AnnotationKey.Kind.database)) ?: return
        update(annotation = annotation, color = (color to viewState.isDark), document = this.document)
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
    }

    private fun setupSearchStateFlow() {
        onSearchStateFlow
            .debounce(150)
            .map { text ->
                search(text)
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

    private fun loadRawDocument() {
        this.rawDocument =
            PdfDocumentLoader.openDocument(context, this.document.documentSource.fileUri)
    }

    private fun setupInteractionListeners() {
        fragment.addOnAnnotationSelectedListener(object :
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
        fragment.addOnAnnotationDeselectedListener { annotation, _ ->
            deselectSelectedAnnotation(annotation)
        }
    }

    private fun initState() {
        val params = ScreenArguments.pdfReaderArgs
        val username = defaults.getUsername()
        val userId = sessionDataEventStream.currentValue()!!.userId
        val displayName = defaults.getDisplayName()

        this.toolColors = mutableMapOf(
            AnnotationTool.HIGHLIGHT to defaults.getHighlightColorHex(),
            AnnotationTool.SQUARE to defaults.getSquareColorHex(),
            AnnotationTool.NOTE to defaults.getNoteColorHex(),
            AnnotationTool.INK to defaults.getInkColorHex(),
        )

        this.activeLineWidth = defaults.getActiveLineWidth()
        this.activeEraserSize = defaults.getActiveEraserSize()

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
            var page: Int = -1
            var results: RealmResults<RItem>? = null
            dbWrapper.realmDbStorage.perform { coordinator ->
                page = coordinator.perform(
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
    ): Map<String, DocumentAnnotation> {
        val annotations = mutableMapOf<String, DocumentAnnotation>()
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
                    documentAnnotations = documentAnnotations
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
                        documentAnnotations = documentAnnotations,
                        sortedKeys = sortedKeys,
                        visiblePage = page,
                        initialPage = null,
                    )
                }

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
        fragment.addOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)
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
                    fieldValues = values
                )
                requests.add(request)
            }
        } else if (hasChanges(listOf(PdfAnnotationChanges.boundingBox, PdfAnnotationChanges.rects))) {
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
            val values = mapOf(KeyBaseKeyPair(key = FieldKeys.Item.Annotation.color, baseKey = null) to annotation.baseColor)
            val request = EditItemFieldsDbRequest(key= key, libraryId = viewState.library.identifier, fieldValues = values)
            requests.add(request)
        }

        if (hasChanges(listOf(PdfAnnotationChanges.contents))) {
            val values = mapOf(KeyBaseKeyPair(key = FieldKeys.Item.Annotation.comment, baseKey = null) to (annotation.contents ?: ""))
            val request = EditItemFieldsDbRequest(key = key, libraryId = viewState.library.identifier, fieldValues = values)
            requests.add(request)
        }

        if(requests.isEmpty()) { return }

        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapper,
                writeRequests = requests
            ).ifFailure {
                Timber.e(it, "PDFReaderViewModel:  can't update changed annotations")
                return@launch
            }
        }

        fragment.notifyAnnotationHasChanged(annotation)
        //TODO
    }

    private fun processAnnotationObserving(
        annotation: Annotation,
        changes: List<String>,
        pdfReaderNotification: PdfReaderNotification
    ) {

        when (pdfReaderNotification) {
            PdfReaderNotification.PSPDFAnnotationChanged -> {
                if (!changes.isEmpty()) {
                    change(annotation = annotation, changes = changes)
                } else {
                    change(
                        annotation = annotation,
                        changes = PdfAnnotationChanges.stringValues(
                            listOf(
                                PdfAnnotationChanges.boundingBox,
                                PdfAnnotationChanges.paths
                            )
                        )
                    )
                }
            }
            PdfReaderNotification.PSPDFAnnotationsAdded -> {
                add(listOf(annotation))
            }
            PdfReaderNotification.PSPDFAnnotationsRemoved -> {
                remove(annotations = listOf(annotation))
            }
        }

        updatePdfChanged(annotation, changes)
    }

    private fun updatePdfChanged(annotation: Annotation, changes: List<String>) {
        if (changes.isEmpty()) {
            return
        }
        //TODO Android's PSDFKit library doesn't seem to have that functionality

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
                val annotation = DatabaseAnnotation(item = item)
                val page = annotation._page ?: storedPage
                val boundingBox =
                    annotation.boundingBox(boundingBoxConverter = boundingBoxConverter)
                return page to (key to (page to boundingBox))
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
        documentAnnotations: Map<String, DocumentAnnotation>
    ): List<AnnotationKey> {
        val keys = mutableListOf<Pair<AnnotationKey, String>>()
        for (item in databaseAnnotations) {
            if (!validate(databaseAnnotation = DatabaseAnnotation(item = item))) {
                continue
            }
            keys.add(
                AnnotationKey(
                    key = item.key,
                    type = AnnotationKey.Kind.database
                ) to item.annotationSortIndex
            )
        }
        for (annotation in documentAnnotations.values) {
            val key = AnnotationKey(key = annotation.key, type = AnnotationKey.Kind.document)
            val index = keys.index(key to annotation.sortIndex, sortedBy = { lData, rData ->
                lData.second.compareTo(rData.second) == 1
            })
            keys.add(element = key to annotation.sortIndex, index = index)
        }
        return keys.map { it.first }
    }

    private fun validate(databaseAnnotation: DatabaseAnnotation): Boolean {
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
            org.zotero.android.database.objects.AnnotationType.image
            -> {
                if (databaseAnnotation.item.rects.isEmpty()) {
                    Timber.i("PDFReaderActionHandler: ${databaseAnnotation.type} annotation ${databaseAnnotation.key} missing rects")
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
        var updatedPdfAnnotations = mutableMapOf<Annotation, DatabaseAnnotation>()
        var deletedPdfAnnotations = mutableListOf<Annotation>()
        var insertedPdfAnnotations = mutableListOf<Annotation>()

        for (index in modifications) {
            if (index >= keys.size) {
                Timber.w("Tried modifying index out of bounds! keys.count=${keys.size}; index=$index; deletions=$deletions; insertions=$insertions; modifications=$modifications")
                continue
            }
            val key = keys[index]
            val item = objects.where().key(key.key).findFirst() ?: continue
            val annotation = DatabaseAnnotation(item = item)

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

            val oldAnnotation = DatabaseAnnotation(item = this.databaseAnnotations!![index]!!)
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

            val annotation = DatabaseAnnotation(item = item)

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
                    viewState.documentAnnotations[key.key]?.sortIndex
                }

                AnnotationKey.Kind.database -> {
                    objects.where().key(key.key).findFirst()?.annotationSortIndex
                }
            }
        }
        for (annotation in viewState.documentAnnotations.values) {
            val key = AnnotationKey(key = annotation.key, type = AnnotationKey.Kind.document)
            val index = keys.index(key, sortedBy = { lKey, rKey ->
                val lSortIndex = getSortIndex(lKey) ?: ""
                val rSortIndex = getSortIndex(rKey) ?: ""
                lSortIndex < rSortIndex
            })
            keys.add(element = key, index = index)
        }
        fragment.removeOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)

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
        if (key == viewState.selectedAnnotationKey) {
            return
        }
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
        val showAnnotationPopup = !forceNotShowAnnotationPopup && !viewState.showSideBar && selectedAnnotation != null
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

    private fun selectAndFocusAnnotationInDocument() {
        val annotation = this.selectedAnnotation
        if (annotation != null) {
            val location = viewState.focusDocumentLocation
            if (location != null) {
                focus(annotation = annotation, location = location, document = this.document)
            } else if (annotation.type != org.zotero.android.database.objects.AnnotationType.ink || fragment.activeAnnotationTool?.toAnnotationType() != AnnotationType.INK) {
                val pageIndex = fragment.pageIndex
                select(annotation = annotation, pageIndex = pageIndex, document = this.document)
            }
        } else {
            select(annotation = null, pageIndex = fragment.pageIndex, document = this.document)
        }
    }

    private fun focus(
        annotation: org.zotero.android.pdf.data.Annotation,
        location: Pair<Int, RectF>,
        document: PdfDocument
    ) {
        val pageIndex = annotation.page
        scrollIfNeeded(pageIndex, true) {
            select(annotation = annotation, pageIndex = pageIndex, document = document)
        }
    }

    private fun scrollIfNeeded(pageIndex: Int, animated: Boolean, completion: () -> Unit) {
        if (fragment.pageIndex == pageIndex) {
            completion()
            return
        }

        if (!animated) {
            fragment.setPageIndex(pageIndex, false)
            completion()
            return
        }
        fragment.setPageIndex(pageIndex, true)
        completion()
    }


    private fun select(
        annotation: org.zotero.android.pdf.data.Annotation?,
        pageIndex: Int,
        document: PdfDocument
    ) {

        //TODO updateSelection

        if (annotation != null) {
            val pdfAnnotation = document.annotation(pageIndex, annotation.key)
            if (pdfAnnotation != null) {
                if (!fragment.selectedAnnotations.contains(pdfAnnotation)) {
                    fragment.setSelectedAnnotation(pdfAnnotation)
                    val zoomScale = fragment.getZoomScale(pageIndex)
                    if (zoomScale > 1.0) {
                        fragment.scrollTo(pdfAnnotation.boundingBox, pageIndex, 100, false)
                    }
                }
            } else {
                if (!fragment.selectedAnnotations.isEmpty()) {
                    fragment.clearSelectedAnnotations()
                }
            }
        } else {
            if (!fragment.selectedAnnotations.isEmpty()) {
                fragment.clearSelectedAnnotations()
            }
        }
    }

    fun annotation(key: AnnotationKey): org.zotero.android.pdf.data.Annotation? {
        when (key.type) {
            AnnotationKey.Kind.database -> {
                return this.databaseAnnotations!!.where().key(key.key).findFirst()
                    ?.let { DatabaseAnnotation(item = it) }
            }

            AnnotationKey.Kind.document -> {
                return viewState.documentAnnotations[key.key]
            }
        }
    }

    private fun update(
        annotation: org.zotero.android.pdf.data.Annotation,
        color: Pair<String, Boolean>? = null,
        lineWidth: Float? = null,
        contents: String? = null,
        document: PdfDocument
    ) {
        val pdfAnnotation = document.annotationProvider.getAnnotations(annotation.page)
            .firstOrNull { it.key == annotation.key } ?: return

        val changes = mutableListOf<PdfAnnotationChanges>()

        if (lineWidth != null && lineWidth.rounded(3) != annotation.lineWidth) {
            changes.add(PdfAnnotationChanges.lineWidth)
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
                isHighlight = (annotation.type == org.zotero.android.database.objects.AnnotationType.highlight),
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
        processAnnotationObserving(
            pdfAnnotation,
            PdfAnnotationChanges.stringValues(changes),
            PdfReaderNotification.PSPDFAnnotationChanged
        )
    }

    private fun update(
        pdfAnnotation: Annotation,
        annotation: DatabaseAnnotation,
        parentKey: String,
        libraryId: LibraryIdentifier,
        isDarkMode: Boolean
    ) {
        val changes = mutableListOf<PdfAnnotationChanges>()

        if (pdfAnnotation.baseColor != annotation.color) {
            val hexColor = annotation.color
            val (color, alpha, blendMode) = AnnotationColorGenerator.color(
                colorHex = hexColor,
                isHighlight = (annotation.type == org.zotero.android.database.objects.AnnotationType.highlight),
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
            org.zotero.android.database.objects.AnnotationType.highlight -> {
                val newBoundingBox =
                    annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)
                if (newBoundingBox != pdfAnnotation.boundingBox.rounded(3)) {
                    pdfAnnotation.boundingBox = newBoundingBox
                    changes.add(PdfAnnotationChanges.boundingBox)

                    (pdfAnnotation as HighlightAnnotation).rects =
                        annotation.rects(boundingBoxConverter = annotationBoundingBoxConverter)
                    changes.add(PdfAnnotationChanges.rects)
                } else {
                    val newRects =
                        annotation.rects(boundingBoxConverter = annotationBoundingBoxConverter)
                    val oldRects =
                        ((pdfAnnotation as HighlightAnnotation).rects).map { it.rounded(3) }
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

            org.zotero.android.database.objects.AnnotationType.image -> {
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

    fun selectAnnotation(key: AnnotationKey) {
        if (!viewState.sidebarEditingEnabled && key != viewState.selectedAnnotationKey) {
            _select(key = key, didSelectInDocument = false)
        }
    }


    fun selectAnnotationFromDocument(key: AnnotationKey) {
        if (!viewState.sidebarEditingEnabled && key != viewState.selectedAnnotationKey) {
            _select(key = key, didSelectInDocument = true)
        }
    }

    private fun deselectSelectedAnnotation(annotation: Annotation) {
//        if (viewState.selectedAnnotationKey?.key == annotation.key ) {
//            _select(key = null, didSelectInDocument = false)
//        }
    }

    val selectedAnnotation: org.zotero.android.pdf.data.Annotation?
        get() {
            return viewState.selectedAnnotationKey?.let { annotation(it) }
        }

    override fun onCleared() {
        fragmentManager.commit(allowStateLoss = true) {
            remove(this@PdfReaderViewModel.fragment)
        }
        fragment.removeOnAnnotationUpdatedListener(onAnnotationUpdatedListener!!)

        EventBus.getDefault().unregister(this)
        liveAnnotations?.removeAllChangeListeners()
        annotationPreviewManager.deleteAll(
            parentKey = viewState.key,
            libraryId = viewState.library.identifier
        )
        annotationPreviewManager.cancelProcessing()
        fileCache.cancelProcessing()

        document.annotationProvider
            .getAllAnnotationsOfTypeAsync(AnnotationsConfig.supported)
            .toList()
            .blockingGet()
            .forEach {
                this.document.annotationProvider.removeAnnotationFromPage(it)
            }
        super.onCleared()
    }

    private fun generatePdfConfiguration(pdfSettings: PDFSettings): PdfConfiguration {

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
        return PdfConfiguration.Builder()
            .scrollDirection(scrollDirection)
            .scrollMode(scrollMode)
            .fitMode(fitMode)
            .layoutMode(pageMode)
            .invertColors(isCalculatedThemeDark)
            .themeMode(themeMode)
//            .disableFormEditing()
//            .disableAnnotationRotation()
//            .setSelectedAnnotationResizeEnabled(false)
            .autosaveEnabled(false)
            .build()
    }

    fun loadPreviews(keys: List<String>) {
        if (keys.isEmpty()) else {
            return
        }

        val isDark = viewState.isDark
        val libraryId = viewState.library.identifier

        for (key in keys) {
            if (annotationPreviewMemoryCache.getBitmap(key) != null) {
                continue
            }
            fileCache.preview(
                key = key,
                parentKey = viewState.key,
                libraryId = libraryId,
                isDark = isDark
            )
        }
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    private fun search(term: String) {
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
                copy(snapshotKeys = null, sortedKeys = snapshot, searchTerm = "", filter = null)
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
            copy(sortedKeys = filteredKeys, searchTerm = term, filter = filter)
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
        annotation: org.zotero.android.pdf.data.Annotation,
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
        annotation: org.zotero.android.pdf.data.Annotation,
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

    fun showFilterPopup() {
        val colors = mutableSetOf<String>()
        val tags = mutableSetOf<Tag>()

        val processAnnotation: (org.zotero.android.pdf.data.Annotation) -> Unit = { annotation ->
            colors.add(annotation.color)
            for (tag in annotation.tags) {
                tags.add(tag)
            }
        }

        for (annotation in this.databaseAnnotations!!) {
            processAnnotation(DatabaseAnnotation(item = annotation))
        }
        for (annotation in this.viewState.documentAnnotations.values) {
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
        filterAnnotations(term = viewState.searchTerm, filter = filter)
    }

    fun navigateToPdfSettings() {
        ScreenArguments.pdfSettingsArgs = PdfSettingsArgs(defaults.getPDFSettings())
        triggerEffect(PdfReaderViewEffect.ShowPdfSettings)
    }

    fun showToolOptions() {
        val tool = this.activeAnnotationTool ?: return

        val colorHex = this.toolColors[tool]
        var size: Float? = null
        when (tool) {
            AnnotationTool.INK -> {
                size = this.activeLineWidth
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
        pdfDocumentBeforeFragmentDestruction = fragment.document!!
        if (isChangingConfigurations) {
            removeFragment()
        }
    }

    fun removeFragment() {
        fragmentManager.commit {
            remove(this@PdfReaderViewModel.fragment)
        }
    }

    private fun replaceFragment() {
        val updatedConfiguration = generatePdfConfiguration(defaults.getPDFSettings())
        this.fragment =
            PdfFragment.newInstance(this.pdfDocumentBeforeFragmentDestruction, updatedConfiguration)
        this.fragment.addDocumentListener(object : DocumentListener {
            override fun onDocumentLoaded(document: PdfDocument) {
                val annotationToReselect = this@PdfReaderViewModel.selectedAnnotation
                if (annotationToReselect != null) {
                    val pdfAnnotation =
                        document.annotation(annotationToReselect.page, annotationToReselect.key)
                    if (pdfAnnotation != null) {
                        fragment.setSelectedAnnotation(pdfAnnotation)
                    }
                }
                setupInteractionListeners()
                observeDocument()
                if (queuedUpPdfReaderColorPickerResult != null) {
                    setToolOptions(
                        hex = queuedUpPdfReaderColorPickerResult!!.colorHex,
                        size = queuedUpPdfReaderColorPickerResult!!.size,
                        tool = queuedUpPdfReaderColorPickerResult!!.annotationTool
                    )
                    queuedUpPdfReaderColorPickerResult = null

                }

            }
        })
        this.fragment.addOnAnnotationCreationModeChangeListener(object:
            AnnotationManager.OnAnnotationCreationModeChangeListener {
            override fun onEnterAnnotationCreationMode(p0: AnnotationCreationController) {
                set(true)
            }

            override fun onChangeAnnotationCreationMode(p0: AnnotationCreationController) {
                set(true)
            }

            override fun onExitAnnotationCreationMode(p0: AnnotationCreationController) {
                set(false)
            }

        })

        fragmentManager.commit {
            replace(containerId, this@PdfReaderViewModel.fragment)
        }
        updateVisibilityOfAnnotations()
    }

    private fun updateVisibilityOfAnnotations() {
        handler.postDelayed({
            val pageIndex = if (fragment.pageIndex == -1) 0 else fragment.pageIndex
            this.document.annotationProvider.getAnnotations(pageIndex).forEach {
                this.fragment.notifyAnnotationHasChanged(it)
            }
        }, 200)
    }

    fun toggleToolbarButton() {
        updateState {
            copy(showCreationToolbar = !viewState.showCreationToolbar)
        }
    }
    fun toggle(tool: AnnotationTool) {
        val color = this.toolColors[tool]
        toggle(annotationTool = tool, color = color)
    }

    fun toggle(annotationTool: AnnotationTool, color: String?) {
        val tool = fragment.activeAnnotationTool

        if (tool != null && tool != AnnotationTool.ERASER && tool != this.toolHistory.lastOrNull()) {
            this.toolHistory.add(tool)
            if (this.toolHistory.size > 2) {
                this.toolHistory.removeAt(0)
            }
        }

        if (fragment.activeAnnotationTool == annotationTool) {
            fragment.exitCurrentlyActiveMode()
            return
        }

//        fragment.enterAnnotationCreationMode(annotationTool)

        var drawColor: Int? = null
        var blendMode: BlendMode? = null

        if (color != null) {
            val (_color, _, bM) = AnnotationColorGenerator.color(
                colorHex = color,
                isHighlight = (annotationTool == AnnotationTool.HIGHLIGHT),
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
        fragment.exitCurrentlyActiveMode()
        when (annotationTool) {
            AnnotationTool.INK -> {
                configureInk(drawColor, this.activeLineWidth)
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

            AnnotationTool.ERASER -> {
                configureEraser(this.activeEraserSize)
            }

            else -> {}
        }
        fragment.enterAnnotationCreationMode(annotationTool)
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    private fun configureNote(drawColor: Int?) {
        if (drawColor == null) {
            return
        }
        fragment.annotationConfiguration
            .put(
                AnnotationTool.NOTE,
                NoteAnnotationConfiguration.builder(context)
                    .setDefaultColor(drawColor)
                    .build()
            )
    }

    private fun configureEraser(activeEraserSize: Float) {
        fragment.annotationConfiguration
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
        fragment.annotationConfiguration
            .put(
                AnnotationTool.HIGHLIGHT,
                MarkupAnnotationConfiguration.builder(context, AnnotationTool.HIGHLIGHT) // Configure which color is used when creating ink annotations.
                    .setDefaultColor(drawColor)
                    .build()
            )

    }

    private fun configureInk(drawColor: Int?, activeLineWidth: Float) {
        if (drawColor == null) {
            return
        }
        fragment.annotationConfiguration
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
        fragment.annotationConfiguration
            .put(
                AnnotationType.SQUARE,
                ShapeAnnotationConfiguration.builder(context, AnnotationType.SQUARE)
                    .setDefaultColor(drawColor)
                    .build()
            )
    }

    val activeAnnotationTool: AnnotationTool? get() {
        return this.fragment.activeAnnotationTool
    }

    fun canUndo() : Boolean {
        return this.fragment.undoManager.canUndo()
    }

    fun canRedo() : Boolean {
        return this.fragment.undoManager.canRedo()
    }

    fun onUndoClick() {
        this.fragment.undoManager.undo()
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    fun onRedoClick() {
        this.fragment.undoManager.redo()
        triggerEffect(PdfReaderViewEffect.ScreenRefresh)
    }

    fun onCloseClick() {
        toggleToolbarButton()
    }

    private fun add(annotations: List<Annotation>) {
        val finalAnnotations = splitIfNeededAndProcess(annotations = annotations)

        if (finalAnnotations.isEmpty()) {
            return
        }
        val request = CreateAnnotationsDbRequest(
            attachmentKey = viewState.key,
            libraryId = viewState.library.identifier,
            annotations = finalAnnotations,
            userId = viewState.userId,
            schemaController = this.schemaController,
            boundingBoxConverter = this.annotationBoundingBoxConverter
        )
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapper,
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
            else -> {
                null
            }
        }
    }

    private fun splitIfNeededAndProcess(annotations: List<Annotation>): List<DocumentAnnotation> {
        var toRemove = mutableListOf<Annotation>()
        var toAdd = mutableListOf<Annotation>()
        var documentAnnotations = mutableListOf<DocumentAnnotation>()

        for (annotation in annotations) {
            val tool = tool(annotation) ?:continue
            val activeColor = this.toolColors[tool] ?: continue
            val activeColorString = activeColor
            val (_, _, blendMode) = AnnotationColorGenerator.color(
                activeColor,
                isHighlight = (annotation is HighlightAnnotation),
                isDarkMode = viewState.isDark
            )
            annotation.blendMode = blendMode ?: BlendMode.NORMAL

            if (annotation.key == null || annotation(AnnotationKey(key = annotation.key!!, type = AnnotationKey.Kind.database)) == null) {
            } else {
                continue
            }
            val splitAnnotations = splitIfNeeded(a = annotation, user = viewState.displayName)

            if (splitAnnotations.size > 1) {
                Timber.i("PdfReaderViewModel: did split annotations into ${splitAnnotations.size}")
                toRemove.add(annotation)
                toAdd.addAll(splitAnnotations)
            }

            documentAnnotations.addAll(
                splitAnnotations.mapNotNull {
                    AnnotationConverter.annotation(
                        this.document,
                        it,
                        color = activeColorString,
                        username = viewState.username,
                        displayName = viewState.displayName,
                        boundingBoxConverter = this.annotationBoundingBoxConverter
                    )
                })

            for (pdfAnnotation in splitAnnotations) {
                this.annotationPreviewManager.store(
                    this.rawDocument,
                    pdfAnnotation,
                    parentKey = viewState.key,
                    libraryId = viewState.library.identifier,
                    isDark = viewState.isDark,
                    annotationMaxSideSize = annotationMaxSideSize
                )
            }
        }
        toRemove.forEach {
            this.document.annotationProvider.removeAnnotationFromPage(it)
        }
        toAdd.forEach {
            this.document.annotationProvider.addAnnotationToPage(it)
        }
        return documentAnnotations
    }

    private fun createAnnotations(
        splitRects: List<List<RectF>>,
        original: HighlightAnnotation,
    ): List<HighlightAnnotation> {
        if (splitRects.size <= 1) {
            return listOf(original)
        }
        return splitRects.map { rects ->
            val new = HighlightAnnotation(original.pageIndex, rects)
            new.boundingBox = AnnotationBoundingBoxCalculator.boundingBox(rects)
            new.alpha = original.alpha
            new.color = original.color
            new.blendMode = original.blendMode
            new.contents = original.contents
            new.customData = JSONObject().put(AnnotationsConfig.keyKey, KeyGenerator.newKey())
            new
        }
    }

    private fun createAnnotations(splitPaths: List<List<List<PointF>>>, original: InkAnnotation): List<InkAnnotation> {
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

    private fun splitIfNeeded(a: Annotation, user: String): List<Annotation> {
        val highlightAnnotation = a as? HighlightAnnotation
        if (highlightAnnotation != null) {
            val rects = highlightAnnotation.rects
            val splitRects = AnnotationSplitter.splitRectsIfNeeded(rects = rects)
            if (splitRects != null) {
                return createAnnotations(splitRects, original = highlightAnnotation)
            }
        }
        val inkAnnotation = a as? InkAnnotation
        if (inkAnnotation != null) {
            val paths = inkAnnotation.lines
            val splitPaths = AnnotationSplitter.splitPathsIfNeeded(paths = paths)
            if (splitPaths != null) {
                return createAnnotations(splitPaths, original = inkAnnotation)
            }
        }

        if (a.key == null) {
            a.creator = user
            a.customData = JSONObject().put(AnnotationsConfig.keyKey, KeyGenerator.newKey())
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
                dbWrapper = dbWrapper,
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
                else -> {
                    //no-op
                }
            }
        }
        var drawColor: Int? = null
        if (hex != null) {
            val (_color, _, bM) = AnnotationColorGenerator.color(
                colorHex = hex,
                isHighlight = (tool == AnnotationTool.HIGHLIGHT),
                isDarkMode = viewState.isDark
            )
            drawColor = _color
        }
        updateAnnotationToolDrawColorAndSize(tool, drawColor = drawColor)
    }

    fun onCommentTextChange(annotationKey: String, comment: String) {
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

    fun onCommentFocusFieldChange(annotationKey: String) {
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

    fun onTagsClicked(annotation: org.zotero.android.pdf.data.Annotation) {
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
                dbWrapper = dbWrapper,
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
    val documentAnnotations: Map<String, DocumentAnnotation> = emptyMap(),
    val sortedKeys: List<AnnotationKey> = emptyList(),
    val snapshotKeys: List<AnnotationKey>? = null,
    var selectedAnnotationCommentActive: Boolean = false,
    val sidebarEditingEnabled: Boolean = false,
    val updatedAnnotationKeys: List<AnnotationKey>? = null,
    val searchTerm: String = "",
    val filter: AnnotationsFilter? = null,
    val showSideBar: Boolean = false,
    val showCreationToolbar: Boolean = false,
    val isColorPickerButtonVisible: Boolean = false,
    val commentFocusKey: String? = null,
    val commentFocusText: String = ""
): ViewState

sealed class PdfReaderViewEffect : ViewEffect {
    object NavigateBack : PdfReaderViewEffect()
    object ShowPdfFilters : PdfReaderViewEffect()
    object ShowPdfSettings : PdfReaderViewEffect()
    object ShowPdfColorPicker: PdfReaderViewEffect()
    data class ShowPdfAnnotationAndUpdateAnnotationsList(val scrollToIndex: Int, val showAnnotationPopup: Boolean): PdfReaderViewEffect()
    object ScreenRefresh: PdfReaderViewEffect()
    object ClearFocus: PdfReaderViewEffect()
    object NavigateToTagPickerScreen: PdfReaderViewEffect()
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
