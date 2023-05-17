package org.zotero.android.pdf

import android.graphics.RectF
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.manager.AnnotationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import okhttp3.internal.toHexString
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.database.objects.FieldKeys
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.objects.UpdatableChangeType
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.ktx.annotation
import org.zotero.android.ktx.baseColor
import org.zotero.android.ktx.index
import org.zotero.android.ktx.isZoteroAnnotation
import org.zotero.android.ktx.key
import org.zotero.android.ktx.rounded
import org.zotero.android.sync.AnnotationColorGenerator
import org.zotero.android.sync.AnnotationConverter
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SessionDataEventStream
import timber.log.Timber
import java.util.EnumSet
import javax.inject.Inject

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val sessionDataEventStream: SessionDataEventStream,
) : BaseViewModel2<PdfReaderViewState, PdfReaderViewEffect>(PdfReaderViewState()) {

    private var liveAnnotations: RealmResults<RItem>? = null
    private var databaseAnnotations: RealmResults<RItem>? = null
    private lateinit var annotationBoundingBoxConverter: AnnotationBoundingBoxConverter
    private lateinit var fragment: PdfFragment
    private var onAnnotationUpdatedListener: AnnotationProvider.OnAnnotationUpdatedListener? = null
    private lateinit var document: PdfDocument

    fun init(document: PdfDocument, fragment: PdfFragment) = initOnce {
        this.fragment = fragment
        this.document = document
        annotationBoundingBoxConverter = AnnotationBoundingBoxConverter(document)
        initState()
        loadDocumentData()
        setupInteractionListeners()
    }

    private fun setupInteractionListeners() {
        fragment.addOnAnnotationSelectedListener(object: AnnotationManager.OnAnnotationSelectedListener {
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
                val type: AnnotationKey.Kind = if(annotation.isZoteroAnnotation) AnnotationKey.Kind.database else AnnotationKey.Kind.document
                selectAnnotationFromDocument(AnnotationKey(key = key, type = type))
            }

        })
        fragment.addOnAnnotationDeselectedListener { annotation, p1 ->
            deselectSelectedAnnotation()
        }
    }

    private fun initState() {
        val params = ScreenArguments.pdfReaderArgs
        val username = defaults.getUsername()
        val userId = sessionDataEventStream.currentValue()!!.userId
        val displayName = defaults.getDisplayName()

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
            var results: RealmResults<RItem> ? = null
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
        library: Library,
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
                color = (pdfAnnotation.color.toHexString() ?: "#000000"),
                library = library,
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
                    library = library,
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
                //TODO store previewes

                val (page, selectedData) = preselectedData(databaseAnnotations = databaseAnnotations!!, storedPage = storedPage, boundingBoxConverter = annotationBoundingBoxConverter)

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
        updateAnnotationsList()
    }

    private fun observeDocument() {
        onAnnotationUpdatedListener = object:
            AnnotationProvider.OnAnnotationUpdatedListener {
            override fun onAnnotationCreated(annotation: Annotation) {
            }

            override fun onAnnotationUpdated(annotation: Annotation) {
                processAnnotationObservingUpdated(annotation, emptyList())
            }

            override fun onAnnotationRemoved(annotation: Annotation) {
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
        //TODO
    }

    private fun processAnnotationObservingUpdated(annotation: Annotation, changes: List<String>) {
        if (!changes.isEmpty()) {
            change(annotation = annotation, changes = changes)
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
                val page = annotation._page ?:  storedPage
                val boundingBox = annotation.boundingBox(boundingBoxConverter = boundingBoxConverter)
                return page to (key to (page to boundingBox))
            }
        }

        val initialPage = viewState.initialPage
        if (initialPage != null && initialPage >= 0 && initialPage < this.document.pageCount ) {
            return initialPage to null
        }

        return storedPage to null
    }

    private fun update(document: PdfDocument, zoteroAnnotations: List<Annotation>, key: String, libraryId: LibraryIdentifier, isDark:Boolean) {
        val allAnnotations = document.annotationProvider.getAllAnnotationsOfType(
            EnumSet.allOf(
                AnnotationType::class.java))
        for (annotation in allAnnotations) {
            annotation.flags =
                EnumSet.copyOf(annotation.flags + AnnotationFlags.LOCKED)
                //TODO store annotations
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


    private fun observe(results: RealmResults<RItem>) {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { objects, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op

                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    update(objects = objects, deletions = deletions, insertions = insertions, modifications = modifications)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "PdfReaderViewModel: could not load results")
                }
            }
        })
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

        var selectKey: AnnotationKey? = null
        var selectionDeleted = false

        var updatedKeys = mutableListOf<AnnotationKey>()
        var updatedPdfAnnotations = mutableMapOf<Annotation, DatabaseAnnotation>()
        var deletedPdfAnnotations = mutableListOf<Annotation>()
        var insertedPdfAnnotations = mutableListOf<Annotation>()

        for (index in modifications) {
            val key = keys[index]
            val item = objects.where().key(key.key).findFirst() ?: continue

            if (canUpdate(key = key, item = item, index = index)) {
                updatedKeys.add(key)
            }

            if (item.changeType != UpdatableChangeType.sync.name) {
                continue
            }

            val annotation = DatabaseAnnotation(item = item)
            val pdfAnnotation = this.document.annotationProvider.getAnnotations(annotation.page)
                .firstOrNull { it.key == key.key } ?: continue
            updatedPdfAnnotations[pdfAnnotation] = annotation
        }

        for (index in deletions.reversed()) {
            val key = keys.removeAt(index)

            if (viewState.selectedAnnotationKey == key) {
                selectionDeleted = true
            }

            val oldAnnotation = DatabaseAnnotation(item = this.databaseAnnotations!![index]!!)
            val pdfAnnotation =
                this.document.annotationProvider.getAnnotations(oldAnnotation.page)
                    .firstOrNull { it.key == oldAnnotation.key } ?: continue
            deletedPdfAnnotations.add(pdfAnnotation)
        }

        for (index in insertions) {
            val item = objects[index]!!
            keys.add(
                element = AnnotationKey(key = item.key, type = AnnotationKey.Kind.database),
                index = index
            )

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
                }
            }
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
                    annotation.flags.remove(AnnotationFlags.READONLY)
                }
                //TODO remove cached entry
            }
            deletedPdfAnnotations.forEach {
                this.document.annotationProvider.removeAnnotationFromPage(it)
            }
        }

        if (!insertedPdfAnnotations.isEmpty()) {
            insertedPdfAnnotations.forEach {
                this.document.annotationProvider.addAnnotationToPage(it)
            }

            //TODO store preview
        }
        observeDocument()
        this.databaseAnnotations = objects.freeze()
        updateAnnotationsList()
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
                    copy(focusDocumentLocation = (annotation.page to annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)))
                }
            }
        } else {
            updateState {
                copy(focusSidebarKey = key)
            }
        }

        if (viewState.sortedKeys.contains(key)) {
            val updatedAnnotationKeys = (viewState.updatedAnnotationKeys ?: emptyList()).toMutableList()
            updatedAnnotationKeys.add(key)
            updateState {
                copy(updatedAnnotationKeys = updatedAnnotationKeys)
            }
        }
        selectAndFocusAnnotationInDocument()
        updateAnnotationsList()
    }

    private fun updateAnnotationsList() {
        val index = viewState.sortedKeys.indexOf(viewState.selectedAnnotationKey)
        triggerEffect(PdfReaderViewEffect.UpdateAnnotationsList(index))
    }

    private fun selectAndFocusAnnotationInDocument() {
        val annotation = this.selectedAnnotation
        if (annotation != null) {
            val location = viewState.focusDocumentLocation
            if (location != null) {
                focus(annotation = annotation, location = location, document = this.document)
            } else if (annotation.type != org.zotero.android.database.objects.AnnotationType.ink || fragment.activeAnnotationTool?.toAnnotationType() != AnnotationType.INK) {
                select(annotation = annotation, pageIndex = fragment.pageIndex, document = this.document)
            }
        } else {
            select(annotation = null, pageIndex = fragment.pageIndex, document = this.document)
        }
    }

    private fun focus(
        annotation: org.zotero.android.pdf.Annotation,
        location: Pair<Int, RectF>,
        document: PdfDocument
    ) {
        val pageIndex = location.first
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
        fragment.setPageIndex(pageIndex, false)
        completion()
    }


    private fun select(annotation: org.zotero.android.pdf.Annotation?, pageIndex: Int, document: PdfDocument) {

        //TODO updateSelection

        if (annotation != null) {
            val pdfAnnotation = document.annotation(pageIndex, annotation.key)
            if (pdfAnnotation != null) {
                if (!fragment.selectedAnnotations.contains(pdfAnnotation)) {
                    fragment.setSelectedAnnotation(pdfAnnotation)
                    fragment.scrollTo(pdfAnnotation.boundingBox, pageIndex, 100, false)
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

    fun annotation(key: AnnotationKey): org.zotero.android.pdf.Annotation? {
        when(key.type) {
            AnnotationKey.Kind.database -> {
                return this.databaseAnnotations!!.where().key(key.key).findFirst()?.let { DatabaseAnnotation(item = it) }
            }
            AnnotationKey.Kind.document -> {
                return viewState.documentAnnotations[key.key]
            }
        }
    }


    private fun update(pdfAnnotation: Annotation, annotation: DatabaseAnnotation, parentKey: String, libraryId: LibraryIdentifier, isDarkMode: Boolean) {
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
            if (blendMode!= null) {
                pdfAnnotation.blendMode = blendMode
            }

            changes.add(PdfAnnotationChanges.color)
        }

        when(annotation.type) {
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
                    val newRects = annotation.rects(boundingBoxConverter = annotationBoundingBoxConverter)
                    val oldRects = ((pdfAnnotation as HighlightAnnotation).rects ?: emptyList()).map{ it.rounded(3) }
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
                    val oldPaths = (inkAnnotation.lines ?: emptyList()).map { points ->
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
                val newBoundingBox = annotation.boundingBox(boundingBoxConverter = annotationBoundingBoxConverter)
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

        //TODO store annotation previews
        processAnnotationObservingUpdated(pdfAnnotation, PdfAnnotationChanges.stringValues(changes))
    }

    private fun canUpdate(key: AnnotationKey, item: RItem, index: Int):Boolean {
        when (item.changeType) {
            UpdatableChangeType.sync.name ->
            return true
            UpdatableChangeType.syncResponse.name ->
            return false
        }

        if (!viewState.selectedAnnotationCommentActive || viewState.selectedAnnotationKey != key) { return true }

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

    fun deselectSelectedAnnotation() {
        _select(key = null, didSelectInDocument = false)
    }

    val selectedAnnotation: org.zotero.android.pdf.Annotation?
        get() {
            return viewState.selectedAnnotationKey?.let { annotation(it) }
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
) : ViewState {
}

sealed class PdfReaderViewEffect : ViewEffect {
    object NavigateBack : PdfReaderViewEffect()
    data class UpdateAnnotationsList(val scrollToIndex: Int): PdfReaderViewEffect()
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
