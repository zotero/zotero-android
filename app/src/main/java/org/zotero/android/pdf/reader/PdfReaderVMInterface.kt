package org.zotero.android.pdf.reader

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentManager
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import org.zotero.android.pdf.cache.AnnotationPreviewMemoryCache
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.sidebar.data.Outline
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderThumbnailRow
import org.zotero.android.pdf.reader.sidebar.data.ThumbnailPreviewMemoryCache

interface PdfReaderVMInterface {

    var annotationMaxSideSize: Int
    val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache
    val thumbnailPreviewMemoryCache: ThumbnailPreviewMemoryCache
    val activeAnnotationTool: AnnotationTool?
    var toolColors: MutableMap<AnnotationTool, String>

    fun init(
        uri: Uri,
        annotationMaxSideSize: Int,
        containerId: Int,
        fragmentManager: FragmentManager,
        isTablet: Boolean,
        backgroundColor: Color,
    )

    fun onTagsClicked(annotation: PDFAnnotation)
    fun onAnnotationSearch(text: String)
    fun onCommentFocusFieldChange(annotationKey: String)
    fun onCommentTextChange(annotationKey: String, comment: String)
    fun onMoreOptionsForItemClicked()
    fun annotation(key: AnnotationKey): PDFAnnotation?
    fun selectAnnotation(key: AnnotationKey)
    fun loadAnnotationPreviews(keys: List<String>)
    fun showFilterPopup()
    fun toggle(tool: AnnotationTool)
    fun showToolOptions()
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    fun onUndoClick()
    fun onRedoClick()
    fun onCloseClick()
    fun setSidebarSliderSelectedOption(optionOrdinal: Int)
    fun onOutlineSearch(text: String)
    fun onOutlineItemTapped(outline: Outline)
    fun onOutlineItemChevronTapped(outline: Outline)
    fun selectThumbnail(row: PdfReaderThumbnailRow)
    fun loadThumbnailPreviews(pageIndex: Int)
    fun hidePdfSearch()
    fun togglePdfSearch()
    fun hidePdfAnnotationView()
    fun hidePdfAnnotationMoreView()
    fun hidePdfSettingsView()
    fun onExportPdf()
    fun onExportAnnotatedPdf()
    fun dismissSharePopup()
    fun onShareButtonTapped()
    fun onCopyCitation()
    fun onCopyBibliography()
    fun hideCopyCitation()
}