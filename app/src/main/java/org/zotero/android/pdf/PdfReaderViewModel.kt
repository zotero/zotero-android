package org.zotero.android.pdf

import android.graphics.RectF
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.document.PdfDocument
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
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.ReadAnnotationsDbRequest
import org.zotero.android.database.requests.ReadDocumentDataDbRequest
import org.zotero.android.database.requests.key
import org.zotero.android.ktx.index
import org.zotero.android.ktx.isZoteroAnnotation
import org.zotero.android.sync.AnnotationConverter
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SessionDataEventStream
import timber.log.Timber
import java.util.EnumSet
import javax.inject.Inject

@HiltViewModel
internal class PdfReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val sessionDataEventStream: SessionDataEventStream,
) : BaseViewModel2<PdfReaderViewState, PdfReaderViewEffect>(PdfReaderViewState()) {

    private var liveAnnotations: RealmResults<RItem>? = null
    private var databaseAnnotations: RealmResults<RItem>? = null
    private lateinit var annotationBoundingBoxConverter: AnnotationBoundingBoxConverter


    fun init(document: PdfDocument) = initOnce {
        annotationBoundingBoxConverter = AnnotationBoundingBoxConverter(document)
        initState(document)
        loadDocumentData()
    }

    private fun initState(document: PdfDocument) {
        val params = ScreenArguments.pdfReaderArgs
        val username = defaults.getUsername()
        val userId = sessionDataEventStream.currentValue()!!.userId
        val displayName = defaults.getDisplayName()

        updateState {
            copy(
                key = params.key,
                library = params.library,
                document = document,
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
                val liveAnnotations = dbResult.value!!.first
                val storedPage = dbResult.value!!.second
                this.liveAnnotations?.removeAllChangeListeners()
                observe(liveAnnotations)
                databaseAnnotations = liveAnnotations.freeze()
                val documentAnnotations = loadAnnotations(
                    viewState.document!!,
                    library = library,
                    username = viewState.username,
                    displayName = viewState.displayName
                )
                val dbToPdfAnnotations = AnnotationConverter.annotations(
                    databaseAnnotations!!,
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
                    document = viewState.document!!,
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
                val annotation = DatabaseAnnotation(item = item)
                val page = annotation._page ?:  storedPage
                val boundingBox = annotation.boundingBox(boundingBoxConverter = boundingBoxConverter)
                return page to (key to (page to boundingBox))
            }
        }

        val initialPage = viewState.initialPage
        if (initialPage != null && initialPage >= 0 && initialPage < viewState.document!!.pageCount ) {
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
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op

                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    val deletions = changeSet.deletions
                    val modifications = changeSet.changes
                    val insertions = changeSet.insertions
                    //TODO implement update
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "PdfReaderViewModel: could not load results")
                }
            }
        })
    }
}

internal data class PdfReaderViewState(
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
    val document: PdfDocument? = null,
    val isDark: Boolean = false,
    val initialPage: Int? = null,
    val visiblePage: Int = 0,
    val focusSidebarKey: AnnotationKey? = null,
    val focusDocumentLocation: Pair<Int, RectF>? = null,
    val documentAnnotations: Map<String, DocumentAnnotation> = emptyMap(),
    val sortedKeys: List<AnnotationKey> = emptyList()

) : ViewState

internal sealed class PdfReaderViewEffect : ViewEffect {
    object NavigateBack : PdfReaderViewEffect()
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
